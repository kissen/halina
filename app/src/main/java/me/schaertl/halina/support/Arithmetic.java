package me.schaertl.halina.support;

public class Arithmetic {
    private Arithmetic() {}

    public static int clamp(long n) {
        if (n > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        if (n < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        return (int) n;
    }
}
