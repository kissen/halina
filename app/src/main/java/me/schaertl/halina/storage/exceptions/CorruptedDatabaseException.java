package me.schaertl.halina.storage.exceptions;

public class CorruptedDatabaseException extends DatabaseException {
    public CorruptedDatabaseException(String what) {
        super(what);
    }
}
