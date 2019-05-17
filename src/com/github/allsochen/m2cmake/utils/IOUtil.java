package com.github.allsochen.m2cmake.utils;

import java.io.*;
import java.nio.channels.Channel;

public class IOUtil {


    public static void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException var2) {
            }

        }
    }

    public static void close(Channel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException var2) {
            }

        }
    }

    public static void close(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException var2) {
            }

        }
    }

    public static void close(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException var2) {
            }

        }
    }

    public static void close(Writer writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException var2) {
            }

        }
    }
}
