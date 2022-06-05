package me.schaertl.halina.support;

/**
 * Thin wrapper around a standard {@link Thread}. Instead of overriding
 * the {@link Task#run} method, implement the actual application logic in
 * {@link Task#execute} and handle errors in {@link Task#on}.
 *
 * Created because I really really realy dislike try/catch blocks.
 */
public abstract class Task extends Thread {
    public final void run() {
        try {
            execute();
        } catch (Exception e) {
            on(e);
        }
    }

    /**
     * Called once the Task is started. Implement your application logic
     * here.
     */
    public abstract void execute() throws Exception;

    /**
     * Called if an exception was thrown in {@link Task#execute}.
     * The default implementation ignores the passed error.
     *
     * @param e Error thrown in {@link Task#execute}.
     */
    public void on(Exception e)
    {
        // do nothing with e...
    }
}
