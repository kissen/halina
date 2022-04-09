package me.schaertl.halina.storage.exceptions;

public class NoDatabaseException extends DatabaseException {
    public NoDatabaseException(String what) {
        super(what);
    }
}
