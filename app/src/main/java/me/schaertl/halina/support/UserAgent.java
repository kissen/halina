package me.schaertl.halina.support;

public class UserAgent {
    private UserAgent() {}

    public static String get() {
        return "halina/prerelease";
    }
}
