package com.github.allsochen.m2cmake.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilterUtil {

    public static boolean excludeIncludeOrSrc(File file) {
        // exclude the directory.
        List<String> excludes = new ArrayList<>();
        excludes.add("test");
        excludes.add("third_party");
        excludes.add("third-party");
        excludes.add("3rd");
        excludes.add("local");
        excludes.add("java");
        excludes.add("go");
        excludes.add("php");
        excludes.add("ruby");
        excludes.add("python");
        excludes.add("csharp");

        for (String exclude : excludes) {
            if (file.getAbsolutePath().contains(File.separator + exclude + File.separator)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIncludeOrSrc(File file, boolean exclude) {
        if (exclude && excludeIncludeOrSrc(file)) {
            return false;
        }
        String fileName = file.getName();
        return "include".equals(fileName) || "src".equals(fileName);
    }
}
