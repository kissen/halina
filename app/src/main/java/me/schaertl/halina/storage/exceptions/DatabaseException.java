package me.schaertl.halina.storage.exceptions;

import java.io.IOException;

public class DatabaseException extends IOException {
    public DatabaseException(String what) {
        super(what);
    }
}
