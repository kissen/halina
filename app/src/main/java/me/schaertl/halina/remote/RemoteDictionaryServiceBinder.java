package me.schaertl.halina.remote;

import android.os.Binder;

public class RemoteDictionaryServiceBinder extends Binder {
    private final RemoteDictionaryService parent;

    public RemoteDictionaryServiceBinder(RemoteDictionaryService service) {
        this.parent = service;
    }

    public RemoteDictionaryService getService() {
        return parent;
    }
}
