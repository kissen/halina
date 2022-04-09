package me.schaertl.halina.remote.structs;

public class Progress {
    public final long createdOn;
    private final long completedSteps;
    private final long totalSteps;

    public Progress(long completedSteps, long totalSteps) {
        this.completedSteps = completedSteps;
        this.totalSteps = totalSteps;
        this.createdOn = System.currentTimeMillis();
    }

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
}
