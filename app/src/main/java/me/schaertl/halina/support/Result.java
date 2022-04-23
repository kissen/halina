package me.schaertl.halina.support;

public class Result<ResultType> {
    private final ResultType result;
    private final Exception error;

    public static <T> Result<T> of(T result) {
        if (result == null) {
            throw new IllegalArgumentException("argument result may not be null");
        }

        return new Result<T>(result, null);
    }

    public static <T> Result<T> error(Exception cause) {
        if (cause == null) {
            throw new IllegalArgumentException("argument cause may not be null");
        }

        return new Result<T>(null, cause);
    }

    public static <T> Result<T> error(String cause) {
        if (cause == null) {
            throw new IllegalArgumentException("argument cause may not be null");
        }

        final Exception exception = new RuntimeException(cause);
        return new Result<T>(null, exception);
    }

    private Result(ResultType result, Exception error) {
        this.result = result;
        this.error = error;
    }

    public boolean isPresent()  {
        return this.result != null;
    }

    public boolean isError() {
        return this.error != null;
    }

    public ResultType get() throws Exception {
        if (this.error != null) {
            throw this.error;
        }

        return this.result;
    }

    public ResultType getResult() {
        if (this.result == null) {
            throw new IllegalStateException("tried to get result that is not present");
        }

        return this.result;
    }

    public Exception getError() {
        if (this.error == null) {
            throw new IllegalStateException("tried to get error when error not present");
        }

        return this.error;
    }

    public String getErrorMessage() {
        if (this.error == null) {
            throw new IllegalStateException("tried to get error when error not present");
        }

        final String type = this.error.getClass().getName();
        final String message = this.error.getMessage();

        return String.format("%s: %s", type, message);
    }
}
