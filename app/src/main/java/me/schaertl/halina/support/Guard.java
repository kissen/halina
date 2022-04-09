package me.schaertl.halina.support;

import java.util.concurrent.locks.Lock;

public class Guard implements AutoCloseable {
    private final Lock lock;

    public Guard(Lock lock) {
        this.lock = lock;
        this.lock.lock();
    }

    @Override
    public void close() {
        lock.unlock();
    }
}
