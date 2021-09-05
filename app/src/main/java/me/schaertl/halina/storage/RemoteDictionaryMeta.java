package me.schaertl.halina.storage;

public class RemoteDictionaryMeta {
    public final String version;
    public final int nbytes;
    public final String url;

    public RemoteDictionaryMeta(String version, int nbytes, String url) {
        this.version = version;
        this.nbytes = nbytes;
        this.url = url;
    }
}
