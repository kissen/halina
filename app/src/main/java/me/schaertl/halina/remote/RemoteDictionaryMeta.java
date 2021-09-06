package me.schaertl.halina.remote;

import org.json.JSONException;
import org.json.JSONObject;

import me.schaertl.halina.support.Result;

public class RemoteDictionaryMeta {
    public final String version;
    public final int nbytes;
    public final String url;

    public static Result<RemoteDictionaryMeta> from(JSONObject json) {
        try {
            final String version = json.getString("version");
            final int nbytes = json.getInt("nbytes");
            final String url = json.getString("url");

            final RemoteDictionaryMeta meta = new RemoteDictionaryMeta(version, nbytes, url);
            return Result.of(meta);
        } catch (JSONException e) {
            return Result.error(e);
        }
    }

    public RemoteDictionaryMeta(String version, int nbytes, String url) {
        this.version = version;
        this.nbytes = nbytes;
        this.url = url;
    }
}
