package me.schaertl.halina.remote.structs;

import me.schaertl.halina.remote.RemoteDictionaryMeta;

public interface RemoteDictionaryHandler {
    void onNewMeta(RemoteDictionaryMeta meta);
    void onNewMetaFailed(String errorMessage);
    void onInstallProgress(String url, Phase phase, Progress progress);
    void onInstallCompleted(String fileLocation);
    void onInstallFailed(String errorMessage);
}
