package me.schaertl.halina.support;

import me.schaertl.halina.support.Progress;

/**
 * Interface implemented by classes that wish to report some kind of concurrent progress.
 */
public interface ProgressHandler {
    /**
     * Called when the implementor wants to communicate some progress.
     * @param currentProgress Progress of the currently running operation.
     */
    void onProgress(Progress currentProgress);
}
