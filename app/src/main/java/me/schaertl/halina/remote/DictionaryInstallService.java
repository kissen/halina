package me.schaertl.halina.remote;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import me.schaertl.halina.MainActivity;
import me.schaertl.halina.R;
import me.schaertl.halina.SettingsActivity;
import me.schaertl.halina.support.Progress;
import me.schaertl.halina.support.ProgressHandler;
import me.schaertl.halina.storage.Storage;
import me.schaertl.halina.support.Arithmetic;
import me.schaertl.halina.support.Fs;
import me.schaertl.halina.support.Gzip;
import me.schaertl.halina.support.Http;
import me.schaertl.halina.support.Task;

/**
 * Background service that can be used to (1) query a backend server for new dictionaries and
 * (2) download and install such a new dictionary.
 */
public class DictionaryInstallService extends Service {
    //
    // Constants.
    //

    public static String BROADCAST_FILTER = "DictionaryInstallService#BROADCAST_FILTER";

    private static final String NOTIFICATION_CHANNEL_ID = "DictionaryInstallService#NOTIFCATION_CHANNEL_ID";
    private static final String NOTIFICATION_CHANNEL_NAME = "DictionaryInstallService";
    private static final int NOTIFICATION_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_LOW;

    private static final int FOREGROUND_ID = 0x6c6f7665;  // arbitrary

    //
    // Public Types.
    //

    public enum State {
        READY,
        PREPARING,
        DOWNLOADING,
        EXTRACTING,
        INSTALLING,
        INSTALLED,
        ERROR
    }

    public class DictionaryInstallBinder extends Binder {
        public DictionaryInstallService getService() {
            return DictionaryInstallService.this;
        }
    }

    public static class Report {
        public final State state;
        public final Exception error;
        public final Progress progress;

        public Report(State state, Exception error, Progress progress) {
            this.state = state;
            this.error = error;
            this.progress = progress;
        }
    }

    //
    // Variables.
    //

    private State state;
    private Exception error;
    private Progress progress;
    private boolean isBound;
    private boolean isShuttingDown;

    private InstallTask installTask;
    private StopTask stopTask;

    private NotificationCompat.Builder notificationBuilder;

    //
    // Static Helpers.
    //

    private static boolean isStopState(State state) {
        switch (state) {
            case READY:
            case INSTALLED:
            case ERROR:
                return true;

            default:
                return false;
        }
    }

    @SuppressLint("DefaultLocale")
    public static String format(Report report) {
        final State state = report.state;
        final String progress = format(report.progress);

        switch (state) {
            case DOWNLOADING:
                return String.format("Downloading... %s", progress);

            case EXTRACTING:
                return String.format("Extracting... %s", progress);

            case INSTALLING:
                return String.format("Installing... %s", progress);

            case INSTALLED:
                return "Installed.";

            case ERROR:
                return format(report.error);

            default:
                return "Ready.";
        }
    }

    @SuppressLint("DefaultLocale")
    private static String format(@Nullable Progress progress) {
        if (progress == null) {
            return "";
        }

        final int percent = Math.round(progress.percent());
        return String.format("(%d%%)", percent);
    }

    @SuppressLint("DefaultLocale")
    private static String format(@Nullable Exception e) {
        if (e == null) {
            return "";
        }

        final String name = e.getClass().getSimpleName();
        final String message = e.getMessage();

        return String.format("%s: %s", name, message);
    }


    //
    // Constructor.
    //

    public DictionaryInstallService() {
        super();

        state = State.READY;
    }

    //
    // Android Lifetime.
    //

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        final Notification notification = updateNotification(null, null);
        startForeground(FOREGROUND_ID, notification);

        return START_STICKY;
    }

    @Override
    public synchronized IBinder onBind(Intent intent) {
        if (stopTask == null) {
            stopTask = new StopTask();
            stopTask.start();
        }

        return new DictionaryInstallBinder();
    }

    @Override
    public synchronized boolean onUnbind(Intent intent) {
        isBound = false;
        return true;
    }

    //
    // Getters.
    //

    public synchronized Report getReport() {
        return new Report(state, error, progress);
    }

    //
    // Notifications.
    //

    public synchronized Notification updateNotification(@Nullable String text, @Nullable Progress progress) {
        // Prepare pending intent. We need to do this so the notification becomes
        // clickable and redirects us to the settings.

        final Intent[] intents = {
            new Intent(this, MainActivity.class),
            new Intent(this, SettingsActivity.class)
        };

        final PendingIntent pending = PendingIntent.getActivities(
            this, 0, intents, PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Now we can create the notification. We have to re-use the notification builder
        // to avoid annoying notifications that focus w/ every update, viz.
        // https://stackoverflow.com/questions/6406730/updating-an-ongoing-notification-quietly

        if (notificationBuilder == null) {
            notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.muse)
                    .setContentTitle("Halina")
                    .setOngoing(true)
                    .setContentIntent(pending)
                    .setOnlyAlertOnce(true);
        }

        if (text == null) {
            notificationBuilder.setContentText("");
        } else {
            notificationBuilder.setContentText(text);
        }

        if (progress == null) {
            notificationBuilder.setProgress(1, 0, true);
        } else {
            final int max = Arithmetic.clamp32(progress.getTotalSteps());
            final int done = Arithmetic.clamp32(progress.getCompletedSteps());
            notificationBuilder.setProgress(max, done, false);
        }

        // Display the notification.

        final Notification notification = notificationBuilder.build();
        final NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(FOREGROUND_ID, notification);

        return notification;
    }

    private void createNotificationChannel() {
        final NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NOTIFICATION_CHANNEL_IMPORTANCE
        );

        final NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }


    //
    // State Management.
    //

    public synchronized void installNewDictionary(Context context, String gzipUrl) {
        if (isShuttingDown) {
            return;
        }

        if (installTask != null) {
            return;
        }

        this.state = State.PREPARING;
        this.error = null;
        this.progress = null;

        this.installTask = new InstallTask(context, gzipUrl);
        this.installTask.start();

        publishStateChange();
    }

    private synchronized void setError(Exception cause) {
        this.state = State.ERROR;
        this.error = cause;
        this.progress = null;
        this.installTask = null;

        publishStateChange();
    }

    private synchronized void setProgress(State state, Progress progress) {
        this.state = state;
        this.error = null;
        this.progress = progress;

        if (isStopState(state)) {
            this.installTask = null;
        }

        publishStateChange();
    }

    private synchronized void publishStateChange() {
        // Update the notification.

        final Report report = getReport();
        final String summary = format(report);
        updateNotification(summary, report.progress);

        // Broadcast that we have some news to share.

        final Intent intent = new Intent(BROADCAST_FILTER);
        sendBroadcast(intent);
    }

    //
    // Logic for Stopping the Service.
    //

    public synchronized void stop() {
        isShuttingDown = true;

        stopForeground(true);
        stopSelf();
    }

    /**
     * Task that repeatedly checks the state of the parent DictionaryInstallService.
     *
     * If the parent remains in a stopped state for some ammount of time, this task
     * kills the service. This is necessary because we do not want an inactive service
     * running in the foreground.
     */
    private class StopTask extends Task {
        private final static int DELAY_MS = 2500;
        private final DictionaryInstallService parent = DictionaryInstallService.this;

        @Override
        public void execute() throws Exception {
            boolean wasInStopState = false;

            while (!parent.isShuttingDown) {
                Thread.sleep(DELAY_MS);

                synchronized (parent) {
                    final boolean inStopState = parentIsInStopState();

                    if (inStopState && wasInStopState) {
                        parent.stop();
                    }

                    wasInStopState = inStopState;
                }
            }
        }

        @Override
        public void on(Exception e) {
            e.printStackTrace();
        }

        private boolean parentIsInStopState() {
            if (parent.isBound) {
                return false;
            }

            return isStopState(parent.state);
        }
    }

    //
    // Actual Download, Extract and Install.
    //

    /**
     * Tasks that downloads, extracts and installs a new dictionary file in the background.
     * That is, InstallTask does all the heavy lifting.
     */
    private class InstallTask extends Task implements ProgressHandler {
        private final Context context;
        private final String downloadUrl;
        private final DictionaryInstallService parent;

        public InstallTask(Context context, String downloadUrl) {
            super();

            this.context = context;
            this.downloadUrl = downloadUrl;
            this.parent = DictionaryInstallService.this;
        }

        @Override
        public void execute() throws Exception {
            final String gzipFile = downloadFile(this.downloadUrl);
            final String sqlite3File = extract(gzipFile);
            install(sqlite3File);

            setProgress(DictionaryInstallService.State.INSTALLED, null);
        }

        @Override
        public void on(Exception e) {
            setError(e);
        }

        @Override
        public void onProgress(Progress currentProgress) {
            synchronized (parent) {
                setProgress(state, currentProgress);
            }
        }

        /**
         * Download file at url to temporary directory. Returns the file location.
         */
        private String downloadFile(String url) throws Exception {
            setProgress(DictionaryInstallService.State.DOWNLOADING, Progress.zero());
            return Http.downloadToTempDirectory(url, this);
        }

        /**
         * Extract GZIP file at path to temporary directory. Returns file location.
         */
        private String extract(String gzipFile) throws Exception {
            setProgress(DictionaryInstallService.State.EXTRACTING, Progress.zero());
            final String tempDir = Fs.createTempDirectory("halina");
            final String extractedFile = Fs.join(tempDir, "dictionary.sqlite3");
            Gzip.extract(gzipFile, extractedFile, this);
            Fs.delete(gzipFile);
            return extractedFile;
        }

        private void install(String sqlite3File) throws Exception {
            setProgress(DictionaryInstallService.State.INSTALLING, null);
            Storage.installNewDictionary(context, sqlite3File);
        }
    }
}