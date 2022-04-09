package me.schaertl.halina.remote;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import me.schaertl.halina.storage.Storage;
import me.schaertl.halina.support.Fs;
import me.schaertl.halina.support.Gzip;
import me.schaertl.halina.support.Result;

public class RemoteDictionaryService extends Service {
    private static volatile RemoteDictionaryService instance;
    private final static List<RemoteDictionaryHandler> listeners = new ArrayList<>();

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
            try {
                this.doUpdate();
            } catch (Exception e) {
                ifLatest(() -> distributeInstallFailed(e.toString()));
            }
        }

        @Override
        public void onProgress(Progress currentProgress) {
            ifLatest(() -> distributeInstallProgress(this.url, Phase.DOWNLOADING, currentProgress));
        }

        private void doUpdate() throws Exception {
            // (1) First download the image from the Halina server.

            final Result<String> fileLocationResult = Http.downloadToTempDirectory(url, this);

            if (fileLocationResult.isError()) {
                final String errorMessage = fileLocationResult.getErrorMessage();
                ifLatest(() -> distributeInstallFailed(errorMessage));
                return;
            }

            // (2) We distribute dictionaries as GZIP-ed files. Here we unzip that file. If
            // unzipping failed, the file is probably corrupt.

            final String gzipFileLocation = fileLocationResult.getResult();

            final String dictDir = Fs.createTempDirectory("halina");
            final String sqlite3FileLocation = Fs.join(dictDir, "dictionary.sqlite3");

            ifLatest(() -> distributeInstallProgress(this.url, Phase.EXTRACTING, null));
            Gzip.extract(gzipFileLocation, sqlite3FileLocation);

            // (3) Now that we have extracted the GZIP file, we can delete the compressed file
            // as we do not need it anymore. We really should do this as early as possible as
            // the files can get quite big for mobile devices.

            Fs.delete(gzipFileLocation);

            // (4) Now that we have extracted the file, we need to pass it to the storage
            // backend for installation.

            final Context context = instance.getApplicationContext();

            ifLatest(() -> distributeInstallProgress(this.url, Phase.VALIDATING, null));
            Storage.installNewDictionary(context, sqlite3FileLocation);

            // (5) Success. Now it is time to rejoice, thank your parents, partner, friends
            // and party hard!

            ifLatest(() -> distributeInstallCompleted(sqlite3FileLocation));
        }

        private void ifLatest(Runnable action) {
            synchronized (DictionaryDownloader.class) {
                action.run();
                lastUpdatedAt = this.startedAt;
            }
        }
    }
}