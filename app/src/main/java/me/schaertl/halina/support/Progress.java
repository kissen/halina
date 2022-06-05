package me.schaertl.halina.support;

/**
 * Represents progress of some long running operation (e.g. file transfer.)
 */
public class Progress {
    private final long completedSteps;
    private final long totalSteps;

    /**
     * @return A {@link Progress} object stuck at 0%.
     */
    public static Progress zero() {
        return new Progress(0, 1);  // 0%
    }

    /**
     * Construct a new progress with given completed out of total steps.
     *
     * @param completedSteps Steps currently completed.
     * @param totalSteps Total number of steps that have to be completed until completion.
     */
    public Progress(long completedSteps, long totalSteps) {
        this.completedSteps = completedSteps;
        this.totalSteps = totalSteps;
    }

    /**
     * Return completion in percent.
     *
     * The returned valued is guaranteed to be some non-NaN float between 0 and 100.
     */
    public float percent() {
        if (this.completedSteps <= 0 || this.totalSteps <= 0) {
            return 0;
        }

        if (this.completedSteps >= this.totalSteps) {
            return 100;
        }

        final float completedReal = completedSteps;
        final float totalReal = totalSteps;

        return (completedReal / totalReal) * 100;
    }

    /**
     * @return Total number of steps that have to be completed until completion.
     */
    public long getTotalSteps() {
        return totalSteps;
    }

    /**
     * @return Steps currently completed.
     */
    public long getCompletedSteps() {
        return completedSteps;
    }
}
