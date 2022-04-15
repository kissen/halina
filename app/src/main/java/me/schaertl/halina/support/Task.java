package me.schaertl.halina.support;

public abstract class Task extends Thread {
    public final void run() {
        try {
            execute();
        } catch (Exception e) {
            on(e);
        }
    }

    public abstract void execute() throws Exception;

    public void on(Exception e)
    {
        // do nothing with e...
    }
}
