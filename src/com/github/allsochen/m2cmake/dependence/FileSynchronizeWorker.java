package com.github.allsochen.m2cmake.dependence;

import com.github.allsochen.m2cmake.BaseAction;
import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.makefile.TafMakefileProperty;
import com.github.allsochen.m2cmake.utils.CollectionUtil;
import com.github.allsochen.m2cmake.utils.Constants;
import com.github.allsochen.m2cmake.utils.FileUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FileSynchronizeWorker {

    private JsonConfig jsonConfig;
    private TafMakefileProperty tafMakefileProperty;
    private String app;
    private String target;

    public FileSynchronizeWorker(JsonConfig jsonConfig, TafMakefileProperty tafMakefileProperty,
                                 String app, String target) {
        this.jsonConfig = jsonConfig;
        this.tafMakefileProperty = tafMakefileProperty;
        this.app = app;
        this.target = target;
    }

    /**
     * point to source path by specified tafjce source directory.
     * /home/tafjce/MTT/AServer => Z:/tafjce/MTT/AServer
     *
     * @param includePath
     * @return
     */
    private List<String> toTafjceRemoteMappingIncludeDir(String includePath) {
        List<String> tafjceMappingRemoteDirs = new ArrayList<>();
        // tafjceSourceDir => Z:/tafjce
        for (String tafjceRemoteDir : jsonConfig.getTafjceRemoteDirs()) {
            tafjceMappingRemoteDirs.add(includePath.replace(Constants.HOME_TAFJCE, tafjceRemoteDir));
        }
        return tafjceMappingRemoteDirs;
    }

    /**
     * /home/tafjce/MTT/AServer => D:/Codes/tafjce/MTT/AServer
     *
     * @param includePath
     * @return
     */
    private String toTafjceLocalMappingIncludeDir(String includePath) {
        String tafjceLocalMappingDir = jsonConfig.getTafjceLocalDir();
        return includePath.replace(Constants.HOME_TAFJCE, tafjceLocalMappingDir);
    }

    private boolean isCopyFileFamily(String fileName) {
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (suffix.equals("h") ||
                suffix.equals("cpp") ||
                suffix.equals("hpp") ||
                suffix.equals("cc") ||
                suffix.equals("jce") ||
                suffix.equals("mk")) {
            return true;
        }
        return false;
    }

    private boolean isCopyPah(String path) {
        for (String remoteDirs : this.jsonConfig.getTafjceRemoteDirs()) {
            if (path.contains(remoteDirs)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDiff(File source, File destination) {
        return destination.lastModified() < source.lastModified();
    }

    private void copyFolder(File source, File destination,
                            ProgressIndicator progressIndicator,
                            int index,
                            int length) {
        if (!source.exists()) {
            return;
        }
        // Ignore file name starts with `.` like `.git/.svn`
        if (source.isDirectory() && !source.getName().startsWith(".")) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] files = source.list();
            for (String fileName : files) {
                StringBuilder message = new StringBuilder();
                String operator = "ignore";
                File srcFile = new File(source, fileName);
                File destFile = new File(destination, fileName);
                if ((isCopyFileFamily(fileName) && hasDiff(srcFile, destFile)) || srcFile.isDirectory()) {
                    operator = "sync";
                    copyFolder(srcFile, destFile, progressIndicator, index, length);
                }
                message.append(index).append("/").append(length).append(": ");
                message.append(operator).append(" ");
                message.append(srcFile.getPath()).append(" to ").append(destination.getPath());
                if (progressIndicator != null && progressIndicator.isRunning()) {
                    progressIndicator.setText2(message.toString());
                }
            }
        } else {
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileUtils.copyFileIfModified(source, destination);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public boolean perform(ProgressIndicator progressIndicator) {
        if (jsonConfig == null) {
            return false;
        }
        if (tafMakefileProperty == null) {
            return false;
        }
        List<String> jceIncludeFilePaths = tafMakefileProperty.getJceDependenceRecurseIncludes(
                this.jsonConfig.getTafjceRemoteDirs(), true);
        // Add itself
        jceIncludeFilePaths.add(Constants.HOME_TAFJCE + "/" + app + "/" + target);
        jceIncludeFilePaths = CollectionUtil.uniq(jceIncludeFilePaths);
        Collections.sort(jceIncludeFilePaths);

        Set<String> copiedDirs = new HashSet<>();
        for (int i = 0; i < jceIncludeFilePaths.size(); i++) {
            String includeFilePath = jceIncludeFilePaths.get(i);
            // /home/tafjce/MTT/AServer
            String includePath = TafMakefileProperty.toIncludePath(includeFilePath);
            List<String> tafjceRemoteMappingIncludeDirs = toTafjceRemoteMappingIncludeDir(includePath);
            String tafjceLocalMappingIncludeDir = toTafjceLocalMappingIncludeDir(includePath);
            if (tafjceLocalMappingIncludeDir.isEmpty()) {
                continue;
            }
            File destDir = new File(tafjceLocalMappingIncludeDir);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            for (String tafjceRemoteMappingIncludeDir : tafjceRemoteMappingIncludeDirs) {
                if (!isCopyPah(tafjceRemoteMappingIncludeDir)) {
                    continue;
                }
                File sourceDir = new File(tafjceRemoteMappingIncludeDir);
                if (!sourceDir.exists()) {
                    continue;
                }
                if (!sourceDir.isDirectory()) {
                    continue;
                }
                if (!copiedDirs.contains(tafjceLocalMappingIncludeDir) ||
                        (destDir.list() == null || destDir.list().length <= 0)) {
                    copyFolder(sourceDir, destDir, progressIndicator, i + 1, jceIncludeFilePaths.size());
                    copiedDirs.add(tafjceLocalMappingIncludeDir);
                }
            }
        }
        return true;
    }
}
