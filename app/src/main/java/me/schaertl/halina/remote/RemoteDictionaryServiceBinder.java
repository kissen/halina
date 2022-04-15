package me.schaertl.halina.remote;

import android.os.Binder;

public class RemoteDictionaryServiceBinder extends Binder {
    private final DictionaryInstallService parent;

    public RemoteDictionaryServiceBinder(DictionaryInstallService service) {
        this.parent = service;
    }

    public DictionaryInstallService getService() {
        return parent;
    }
}
