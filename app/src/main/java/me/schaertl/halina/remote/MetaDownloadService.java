package me.schaertl.halina.remote;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import org.json.JSONObject;

import me.schaertl.halina.remote.structs.RemoteDictionaryMeta;
import me.schaertl.halina.support.Http;
import me.schaertl.halina.support.Result;
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
        public void execute() {
            final Result<JSONObject> json;
            final Result<RemoteDictionaryMeta> meta;

            final String addr = "https://halina.schaertl.me/dictionaries/halina-meta.json";

            if ((json = Http.getJson(addr)).isError()) {
                setError(json.getError());
                return;
            }

            if ((meta = RemoteDictionaryMeta.from(json.getResult())).isError()) {
                setError(meta.getError());
                return;
            }

            setResult(meta.getResult());
        }

        @Override
        public void on(Exception e) {
            setError(e);
        }
    }
}
