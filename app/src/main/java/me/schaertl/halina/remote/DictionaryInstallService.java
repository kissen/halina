package me.schaertl.halina.remote;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import me.schaertl.halina.remote.structs.Progress;
import me.schaertl.halina.remote.structs.ProgressHandler;
import me.schaertl.halina.storage.Storage;
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
    private InstallTask task;
    private Progress progress;

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
    public IBinder onBind(Intent intent) {
        return new DictionaryInstallBinder();
    }

    //
    // Getters.
    //

    public synchronized Report getReport() {
        return new Report(state, error, progress);
    }

    //
    // State Management.
    //

    public synchronized void installNewDictionary(Context context, String gzipUrl) {
        if (task != null) {
            return;
        }

        this.state = State.PREPARING;
        this.error = null;
        this.progress = null;

        this.task = new InstallTask(context, gzipUrl);
        this.task.start();

        triggerBroadcast();
    }

    private synchronized void setError(Exception cause) {
        this.state = State.ERROR;
        this.error = cause;
        this.progress = null;
        this.task = null;

        triggerBroadcast();
    }

    private synchronized void setProgress(State state, Progress progress) {
        this.state = state;
        this.error = null;
        this.progress = progress;

        triggerBroadcast();
    }

    private synchronized void triggerBroadcast() {
        final Intent intent = new Intent(BROADCAST_FILTER);
        sendBroadcast(intent);
    }

    /**
     * Tasks that downloads, extracts and installs a new dictionary file.
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
            return Http.downloadToTempDirectory(url, this).get();
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