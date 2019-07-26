package com.github.allsochen.m2cmake.dependence;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.makefile.TafMakefileProperty;
import com.github.allsochen.m2cmake.utils.CollectionUtil;
import com.github.allsochen.m2cmake.utils.Constants;
import com.github.allsochen.m2cmake.utils.FileUtils;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FileSynchronizeWorker {

    private JsonConfig jsonConfig;
    private TafMakefileProperty tafMakefileProperty;
    private String app;
    private String target;
    private Project project;

    private ExecutorService executor;
    private List<CompletableFuture<Boolean>> futures = new ArrayList<>();
    private AtomicInteger lastIndex = new AtomicInteger(0);

    public FileSynchronizeWorker(JsonConfig jsonConfig, TafMakefileProperty tafMakefileProperty,
                                 String app, String target, Project project) {
        this.jsonConfig = jsonConfig;
        this.tafMakefileProperty = tafMakefileProperty;
        this.app = app;
        this.target = target;
        this.project = project;
        executor = Executors.newFixedThreadPool(5);
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
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
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

    private boolean isCopyPath(String path) {
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
                            final int index,
                            int length) {
        if (!source.exists()) {
            return;
        }
        // Ignore file name starts with `.` like `.git/.svn`
        if (source.getName().startsWith(".")) {
            return;
        }
        if (source.isDirectory()) {
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
                if (index > lastIndex.intValue()) {
                    lastIndex.getAndSet(index);
                }
                message.append(operator).append(" ");
                message.append(srcFile.getPath()).append(" to ").append(destination.getPath());
                if (progressIndicator != null && progressIndicator.isRunning()) {
                    progressIndicator.setText("TAF dependence recurse synchronize...(" +
                            lastIndex.intValue() + "/" + length + ")");
                    progressIndicator.setText2(message.toString());
                }
            }
        } else {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return FileUtils.copyFileIfModified(source, destination);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }, executor));
        }
    }

    public void trySyncTafjceDependenceDirectory(ProgressIndicator progressIndicator, int index, int length) {
        try {
            List<String> paths = WebServersParser.parse(project.getBasePath());
            for (String path : paths) {
                File sourceDir = new File(path + File.separator + Constants.TAFJCE_DEPEND);
                File destDir = new File(ProjectUtil.getTafjceDependenceDir(jsonConfig, target));
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    copyFolder(sourceDir, destDir, progressIndicator, index, length);
                    return true;
                }, executor);
                futures.add(future);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean perform(ProgressIndicator progressIndicator) {
        if (jsonConfig == null) {
            return false;
        }
        if (tafMakefileProperty == null) {
            return false;
        }
        if (progressIndicator != null) {
            progressIndicator.setText2("compute the dependence...");
        }
        List<String> jceIncludeFilePaths = tafMakefileProperty.getJceDependenceRecurseIncludes(
                jsonConfig.getTafjceRemoteDirs(), false);
        // Add itself
        jceIncludeFilePaths.add(Constants.HOME_TAFJCE + "/" + app + "/" + target);
        jceIncludeFilePaths = CollectionUtil.uniq(jceIncludeFilePaths);
        Collections.sort(jceIncludeFilePaths);

        if (progressIndicator != null) {
            progressIndicator.setText2("start sync dependence files...");
        }

        int length = jceIncludeFilePaths.size();
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
                final int index = i + 1;
                if (!isCopyPath(tafjceRemoteMappingIncludeDir)) {
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
                    CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                        copyFolder(sourceDir, destDir, progressIndicator, index, length);
                        return true;
                    }, executor);
                    copiedDirs.add(tafjceLocalMappingIncludeDir);
                    futures.add(future);
                }
            }
        }
        trySyncTafjceDependenceDirectory(progressIndicator, 0, length);
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        return true;
    }
}
