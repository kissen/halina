package me.schaertl.halina.remote;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import me.schaertl.halina.support.Result;

public class RemoteDictionaryService extends Service {
    private static volatile RemoteDictionaryService instance;
    private static List<RemoteDictionaryHandler> listeners;

    static {
        listeners = new ArrayList<>();
    }

    public static Optional<RemoteDictionaryService> getInstance() {
        return Optional.ofNullable(instance);
    }

    public RemoteDictionaryService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static synchronized void registerHandlerFrom( RemoteDictionaryHandler handler) {
        if (!listeners.contains(handler)) {
            listeners.add(handler);
        }
    }

    public void startNewMetaDownload() {
        final Thread worker = new MetaChecker();
        worker.start();
    }

    public void startNewDownloadFrom(@NonNull String url) {
        final Thread worker = new DictionaryDownloader(url);
        worker.start();
    }

    private static synchronized void distributeNewMeta(RemoteDictionaryMeta meta) {
        for (final RemoteDictionaryHandler handler : listeners) {
            handler.onNewMeta(meta);
        }
    }

    private static synchronized void distributeNewMetaFailed(String errorMessage) {
        for (final RemoteDictionaryHandler handler : listeners) {
            handler.onNewMetaFailed(errorMessage);
        }
    }

    private static synchronized void distributeInstallProgress(String url, Phase phase, Progress progress) {
        for (final RemoteDictionaryHandler handler : listeners) {
            handler.onInstallProgress(url, phase, progress);
        }
    }

    private static synchronized void distributeInstallCompleted(String fileLocation) {
        for (final RemoteDictionaryHandler handler : listeners) {
            handler.onInstallCompleted(fileLocation);
        }
    }

    private static synchronized void distributeInstallFailed(String errorMessage) {
        for (final RemoteDictionaryHandler handler : listeners) {
            handler.onInstallFailed(errorMessage);
        }
    }

    private static class MetaChecker extends Thread {
        private static final String REMOTE_ADDR = "https://halina.schaertl.me/dictionaries/halina-meta.json";
        private static long lastUpdatedAt;

        private final long startedAt;

        public MetaChecker() {
            this.startedAt = System.currentTimeMillis();
        }

        @Override
        public void run() {
            // First we have to download the JSON.

            final Result<JSONObject> metaJson = Http.getJson(REMOTE_ADDR);

            if (metaJson.isError()) {
                final String errorMessage = metaJson.getErrorMessage();
                ifLatest(() -> distributeNewMetaFailed(errorMessage));
                return;
            }

            // Now that we have the JSON, we can extract the meta information
            // from it.

            final JSONObject json = metaJson.getResult();
            final Result<RemoteDictionaryMeta> metaObj = RemoteDictionaryMeta.from(json);

            if (metaObj.isError()) {
                final String errorMessage = metaObj.getErrorMessage();
                ifLatest(() -> distributeNewMetaFailed(errorMessage));
                return;
            }

            // Well that worked! Great!

            final RemoteDictionaryMeta meta = metaObj.getResult();
            ifLatest(() -> distributeNewMeta(meta));
        }

        private void ifLatest(Runnable action) {
            synchronized (MetaChecker.class) {
                if (lastUpdatedAt < this.startedAt) {
                    action.run();
                    lastUpdatedAt = this.startedAt;
                }
            }
        }
    }

    private static class DictionaryDownloader extends Thread implements ProgressHandler {
        private static long lastUpdatedAt;

        private final String url;
        private final long startedAt;

        public DictionaryDownloader(String url) {
            super();

            this.url = url;
            this.startedAt = System.currentTimeMillis();
        }

        @Override
        public void run() {
            final Result<String> fileLocationResult = Http.downloadToTempDirectory(url, this);

            if (fileLocationResult.isError()) {
                final String errorMessage = fileLocationResult.getErrorMessage();
                ifLatest(() -> distributeInstallFailed(errorMessage));
                return;
            }

            // TODO: Check archive, install as dictionary.

            final String fileLocation = fileLocationResult.getResult();
            ifLatest(() -> distributeInstallCompleted(fileLocation));
        }

        @Override
        public void onProgress(Progress currentProgress) {
            ifLatest(() -> distributeInstallProgress(this.url, Phase.DOWNLOADING, currentProgress));
        }

        private void ifLatest(Runnable action) {
            synchronized (DictionaryDownloader.class) {
                action.run();
                lastUpdatedAt = this.startedAt;
            }
        }
    }
}
