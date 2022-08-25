package com.github.allsochen.m2cmake.utils;

public class OsInfo {
    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }
}
