package me.schaertl.halina.support;

public class Arithmetic {
    private Arithmetic() {}

    /**
     * Clamp 64 bit integer n such that it fits in a 32 bit integer. If n is too big
     * (or small) to fit, the maximum (or minimum) integer value is returned instead.
     */
    public static int clamp32(long n) {
        if (n > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        if (n < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        return (int) n;
    }
}
