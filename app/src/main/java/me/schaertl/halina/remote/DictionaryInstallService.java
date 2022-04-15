package me.schaertl.halina.remote;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.json.JSONObject;

import me.schaertl.halina.remote.structs.Phase;
import me.schaertl.halina.remote.structs.Progress;
import me.schaertl.halina.remote.structs.ProgressHandler;
import me.schaertl.halina.remote.structs.RemoteDictionaryMeta;
import me.schaertl.halina.storage.Storage;
import me.schaertl.halina.support.Fs;
import me.schaertl.halina.support.Gzip;
import me.schaertl.halina.support.Http;
import me.schaertl.halina.support.Result;

/**
 * Background service that can be used to (1) query a backend server for new dictionaries and
 * (2) download and install such a new dictionary.
 */
public class DictionaryInstallService extends Service {
    //
    // Android Service Event Handlers
    //

    @Override
    public IBinder onBind(Intent intent) {
        return new RemoteDictionaryServiceBinder(this);
    }

    public void startNewMetaDownload() {
        final Thread worker = new MetaDownloadTask();
        worker.start();
    }

    public void startNewDownloadFrom(String url, Context context) {
        final Thread worker = new DictionaryInstallTask(context, url);
        worker.start();
    }

    //
    // Custom Event Handlers.
    //
    // These get called by the various worker threads.
    //

    private static synchronized void distributeNewMeta(RemoteDictionaryMeta meta) {
    }

    private static synchronized void distributeNewMetaFailed(String errorMessage) {
    }

    private static synchronized void distributeInstallProgress(String url, Phase phase, Progress progress) {
    }

    private static synchronized void distributeInstallCompleted(String fileLocation) {
    }

    private static synchronized void distributeInstallFailed(String errorMessage) {
    }

    /**
     * Tasks that fetches JSON meta data from a backend server.
     * The meta data data contains information about available dictionaries.
     */
    private static class MetaDownloadTask extends Thread {
        private static final String REMOTE_ADDR = "https://halina.schaertl.me/dictionaries/halina-meta.json";

        @Override
        public void run() {
            // First we have to download the JSON.

            final Result<JSONObject> metaJson = Http.getJson(REMOTE_ADDR);

            if (metaJson.isError()) {
                final String errorMessage = metaJson.getErrorMessage();
                distributeNewMetaFailed(errorMessage);
                return;
            }

            // Now that we have the JSON, we can extract the meta information
            // from it.

            final JSONObject json = metaJson.getResult();
            final Result<RemoteDictionaryMeta> metaObj = RemoteDictionaryMeta.from(json);

            if (metaObj.isError()) {
                final String errorMessage = metaObj.getErrorMessage();
                distributeNewMetaFailed(errorMessage);
                return;
            }

            // Well that worked! Great!

            final RemoteDictionaryMeta meta = metaObj.getResult();
            distributeNewMeta(meta);
        }
    }

    /**
     * Tasks that downloads, extracts and installs a new dictionary file.
     */
    private static class DictionaryInstallTask extends Thread implements ProgressHandler {
        /** Context of caller. */
        private final Context context;

        /** URL to download the GZIP from. */
        private final String url;

        public DictionaryInstallTask(Context context, String url) {
            super();

            this.context = context;
            this.url = url;
        }

        @Override
        public void run() {
            try {
                this.doUpdate();
            } catch (Exception e) {
                distributeInstallFailed(e.toString());
            }
        }

        @Override
        public void onProgress(Progress currentProgress) {
            distributeInstallProgress(this.url, Phase.DOWNLOADING, currentProgress);
        }

        private void doUpdate() throws Exception {
            // (1) First download the image from the Halina server.

            final Result<String> fileLocationResult = Http.downloadToTempDirectory(url, this);

            if (fileLocationResult.isError()) {
                final String errorMessage = fileLocationResult.getErrorMessage();
                distributeInstallFailed(errorMessage);
                return;
            }

            // (2) We distribute dictionaries as GZIP-ed files. Here we unzip that file. If
            // unzipping failed, the file is probably corrupt.

            final String gzipFileLocation = fileLocationResult.getResult();

            final String dictDir = Fs.createTempDirectory("halina");
            final String sqlite3FileLocation = Fs.join(dictDir, "dictionary.sqlite3");

            distributeInstallProgress(this.url, Phase.EXTRACTING, null);
            Gzip.extract(gzipFileLocation, sqlite3FileLocation);

            // (3) Now that we have extracted the GZIP file, we can delete the compressed file
            // as we do not need it anymore. We really should do this as early as possible as
            // the files can get quite big for mobile devices.

            Fs.delete(gzipFileLocation);

            // (4) Now that we have extracted the file, we need to pass it to the storage
            // backend for installation.

            distributeInstallProgress(this.url, Phase.VALIDATING, null);
            Storage.installNewDictionary(context, sqlite3FileLocation);

            // (5) Success. Now it is time to rejoice, thank your parents, partner, friends
            // and party hard!

            distributeInstallCompleted(sqlite3FileLocation);
        }
    }
}