package me.schaertl.halina.remote;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import org.json.JSONException;
import org.json.JSONObject;

import me.schaertl.halina.support.Http;
import me.schaertl.halina.support.Task;

public class MetaDownloadService extends Service {
    //
    // Constants.
    //

    public static String BROADCAST_FILTER = "MetaDownloadService#BROADCAST_FILTER";

    //
    // Public Types.
    //

    public enum State {
        READY,
        DOWNLOADING,
        DOWNLOADED,
        ERROR
    }

    public class MetaDownloadBinder extends Binder {
        public MetaDownloadService getService() {
            return MetaDownloadService.this;
        }
    }

    public static class Report {
        public final State state;
        public final Exception error;
        public final RemoteDictionaryMeta meta;

        public Report(State state, Exception error, RemoteDictionaryMeta meta) {
            this.state = state;
            this.error = error;
            this.meta = meta;
        }
    }

    //
    // Variables.
    //

    private State state;
    private Exception error;
    private DownloadTask task;
    private RemoteDictionaryMeta meta;

    //
    // Constructor.
    //

    public MetaDownloadService() {
        super();
        state = State.READY;
    }

    //
    // Android Lifetime.
    //

    @Override
    public IBinder onBind(Intent intent) {
        return new MetaDownloadBinder();
    }

    //
    // Getters.
    //

    public synchronized Report getReport() {
        return new Report(state, error, meta);
    }

    //
    // State Management.
    //

    public synchronized void startMetaDownload() {
        if (this.state == State.DOWNLOADING) {
            return;
        }

        this.state = State.DOWNLOADING;
        this.error = null;
        this.meta = null;

        this.task = new DownloadTask();
        this.task.start();

        triggerBroadcast();
    }

    private synchronized void setError(Exception cause) {
        this.state = State.ERROR;
        this.error = cause;
        this.task = null;
        this.meta = null;

        triggerBroadcast();
    }

    private synchronized void setResult(RemoteDictionaryMeta meta) {
        this.state = State.DOWNLOADED;
        this.error = null;
        this.task = null;
        this.meta = meta;

        triggerBroadcast();
    }

    private synchronized void triggerBroadcast() {
        final Intent intent = new Intent(BROADCAST_FILTER);
        sendBroadcast(intent);
    }

    //
    // Background Task.
    //

    private class DownloadTask extends Task {
        @Override
        public void execute() throws Exception {
            final String url = "https://halina.schaertl.me/dictionaries/halina-meta.json";
            final JSONObject json = Http.getJson(url);
            final RemoteDictionaryMeta meta = RemoteDictionaryMeta.from(json);

            setResult(meta);
        }

        @Override
        public void on(Exception e) {
            setError(e);
        }
    }

    /**
     * Represents the metadata provided by some server from which we might
     * want to download a dictionary database.
     */
    public static class RemoteDictionaryMeta {
        /**
         * Version of the dictionary. Should be a string parsable as RFC3399 date/time string.
         */
        public final String version;

        /**
         * File size of given compressed dictionary in bytes.
         */
        public final int nbytes;

        /**
         * HTTP location of the dictionary database.
         */
        public final String url;

        /**
         * Parse JSON meta information.
         *
         * @param json The JSON object that contains the meta information.
         * @return The parsed meta information.
         * @throws JSONException If the JSON format was not as expected.
         */
        public static RemoteDictionaryMeta from(JSONObject json) throws JSONException {
            final String version = json.getString("version");
            final int nbytes = json.getInt("nbytes");
            final String url = json.getString("url");

            return new RemoteDictionaryMeta(version, nbytes, url);
        }

        private RemoteDictionaryMeta(String version, int nbytes, String url) {
            this.version = version;
            this.nbytes = nbytes;
            this.url = url;
        }
    }
}
