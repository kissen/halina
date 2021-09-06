package me.schaertl.halina.remote;

public class UserAgent {
    private UserAgent() {}

    public static String get() {
        return "halina/prerelease";
    }
}
