package com.github.allsochen.m2cmake.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {

    public static boolean copyFileIfModified(File source, File destination) throws IOException {
        if (destination.lastModified() < source.lastModified()) {
            copyFile(source, destination);
            return true;
        } else {
            return false;
        }
    }

    public static void copyFile(File source, File destination) throws IOException {
        String message;
        if (!source.exists()) {
            message = "File " + source + " does not exist";
            throw new IOException(message);
        } else if (!source.getCanonicalPath().equals(destination.getCanonicalPath())) {
            mkdirsFor(destination);
            doCopyFile(source, destination);
            if (source.length() != destination.length()) {
                message = "Failed to copy full contents from " + source + " to " + destination;
                throw new IOException(message);
            }
        }
    }

    private static void mkdirsFor(File destination) {
        File parentFile = destination.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }

    }

    private static void doCopyFile(File source, File destination) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;

        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination);
            input = fis.getChannel();
            output = fos.getChannel();
            long size = input.size();
            long pos = 0L;

            for(long count = 0L; pos < size; pos += output.transferFrom(input, pos, count)) {
                count = size - pos > 31457280L ? 31457280L : size - pos;
            }
        } finally {
            IOUtil.close(output);
            IOUtil.close(fos);
            IOUtil.close(input);
            IOUtil.close(fis);
        }
    }

}
