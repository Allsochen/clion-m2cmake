package com.github.allsochen.m2cmake.dependence;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.makefile.BazelFunctional;
import com.github.allsochen.m2cmake.makefile.BazelWorkspace;
import com.github.allsochen.m2cmake.makefile.TafMakefileProperty;
import com.github.allsochen.m2cmake.utils.CollectionUtil;
import com.github.allsochen.m2cmake.utils.Constants;
import com.github.allsochen.m2cmake.utils.FileUtils;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.github.allsochen.m2cmake.view.ConsoleWindow;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class FileSynchronizeWorker {

    private JsonConfig jsonConfig;
    private TafMakefileProperty tafMakefileProperty;
    private BazelWorkspace bazelWorkspace;
    private String app;
    private String target;
    private Project project;
    private ConsoleWindow consoleWindow;

    private ExecutorService executor;
    private List<CompletableFuture<Boolean>> futures = new ArrayList<>();
    private AtomicInteger lastIndex = new AtomicInteger(0);
    private AtomicInteger ignoreFileCount = new AtomicInteger(0);
    private AtomicInteger syncFileCount = new AtomicInteger(0);

    public FileSynchronizeWorker(JsonConfig jsonConfig, TafMakefileProperty tafMakefileProperty,
                                 BazelWorkspace bazelWorkspace,
                                 String app, String target,
                                 Project project) {
        this.jsonConfig = jsonConfig;
        this.tafMakefileProperty = tafMakefileProperty;
        this.bazelWorkspace = bazelWorkspace;
        this.app = app;
        this.target = target;
        this.project = project;
        this.consoleWindow = ConsoleWindow.getInstance(project);
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
                suffix.equals("log") ||
                suffix.equals("mk") ||
                suffix.equals("bzl") ||
                suffix.equals("md") ||
                suffix.equals("yml")) {
            return true;
        } else {
            String name = fileName.toUpperCase();
            if (name.equals("WORKSPACE") ||
                    name.equals("BUILD")) {
                return true;
            }
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

    private boolean isIgnore(File file) {
        String name = file.getName();
        if (name.startsWith(".") ||
                name.equals("_objs") ||
                name.equals("bazel-bin") ||
                name.equals("bazel-out")) {
            return true;
        }
        return false;
    }

    private void copyFolder(File source, File destination,
                            ProgressIndicator progressIndicator,
                            final int index,
                            int length) {
        if (!source.exists()) {
            return;
        }
        if (isIgnore(source)) {
            consoleWindow.println("Ignore file: " + source.getPath(),
                    ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            return;
        }
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
                consoleWindow.println("CREATE\t" + destination.getPath(),
                        ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            }
            String[] files = source.list();
            if (files == null) {
                return;
            }
            for (String fileName : files) {
                StringBuilder message = new StringBuilder();
                String operator = "IGNORE";
                File srcFile = new File(source, fileName);
                File destFile = new File(destination, fileName);
                if ((isCopyFileFamily(fileName) && hasDiff(srcFile, destFile)) || srcFile.isDirectory()) {
                    operator = "SYNC";
                    if (!srcFile.isDirectory()) {
                        syncFileCount.getAndIncrement();
                    }
                    copyFolder(srcFile, destFile, progressIndicator, index, length);
                } else {
                    ignoreFileCount.getAndIncrement();
                }
                if (index > lastIndex.intValue()) {
                    lastIndex.getAndSet(index);
                }
                message.append(operator).append("\t");
                message.append(srcFile.getPath()).append(" To ").append(destination.getPath());
                if (progressIndicator != null && progressIndicator.isRunning()) {
                    progressIndicator.setText("TAF dependence recurse synchronize...(" +
                            lastIndex.intValue() + "/" + length + ")");
                    progressIndicator.setText2(message.toString());
                }
                if (operator.equals("IGNORE")) {
                    consoleWindow.println(message.toString(), ConsoleViewContentType.LOG_WARNING_OUTPUT);
                } else {
                    consoleWindow.println(message.toString(), ConsoleViewContentType.NORMAL_OUTPUT);
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

    private void trySyncTafjceDependenceDirectory(ProgressIndicator progressIndicator, int length) {
        try {
            List<String> paths = WebServersParser.parse(project.getBasePath());
            for (String path : paths) {
                Stream<Path> walk = Files.walk(Paths.get(path));
                walk.filter(path1 -> path1.getFileName().endsWith(Constants.TAFJCE_DEPEND))
                        .forEach(tafJceDepend -> {
                            File sourceDir = tafJceDepend.toFile();
                            File destDir = new File(ProjectUtil.getTafjceDependenceDir(jsonConfig, target));
                            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                                copyFolder(sourceDir, destDir, progressIndicator, 0, length);
                                return true;
                            }, executor);
                            futures.add(future);
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void trySyncBazelDependenceDirectory(ProgressIndicator progressIndicator) {
        List<BazelFunctional> functionals = bazelWorkspace.getDependencies();
        String workspaceName = bazelWorkspace.getTarget();
        try {
            List<String> syncPaths = WebServersParser.parse(project.getBasePath());
            for (String syncPath : syncPaths) {
                Stream<Path> walk = Files.walk(Paths.get(syncPath));
                File rootFile = new File(syncPath);
                if (!rootFile.exists() || !rootFile.isDirectory()) {
                    consoleWindow.println("WARMING: remote synchronize path is empty. " +
                                    rootFile.getAbsolutePath(),
                            ConsoleViewContentType.ERROR_OUTPUT);
                    continue;
                }
                List<File> targetFiles = new ArrayList<>();
                File[] files = rootFile.listFiles();
                if (files == null) {
                    continue;
                }
                for (File file : files) {
                    String fileName = file.getName();
                    File[] subFiles = file.listFiles();
                    if (fileName.equals(Constants.BAZEL_GENFILES)) {
                        targetFiles.add(file);
                    } else {
                        // Add bazel-workspaceName/external directory.
                        if (fileName.toLowerCase().equals("bazel-" + workspaceName.toLowerCase()) &&
                                subFiles != null) {
                            for (File subFile : subFiles) {
                                if (subFile.isDirectory() && subFile.getName().equals("external")) {
                                    targetFiles.add(subFile);
                                }
                            }
                        }
                    }
                }

                if (targetFiles.isEmpty()) {
                    consoleWindow.println("WARMING: Not found bazel-genfiles or bazel-" +
                                    workspaceName + " directory. " +
                                    "Please execute `bazel build ...` command on remote server " +
                                    "before synchronize dependence.",
                            ConsoleViewContentType.ERROR_OUTPUT);
                }
                int length = targetFiles.size();
                for (int i = 0; i < targetFiles.size(); i++) {
                    File sourceDir = targetFiles.get(i);
                    int index = i + 1;
                    // jce gen files
                    File destDir;
                    if (sourceDir.getName().equals(Constants.BAZEL_GENFILES)) {
                        destDir = new File(ProjectUtil.getBazelGenFilesPath(jsonConfig));
                    } else {
                        destDir = new File(ProjectUtil.getBazelRepositoryExternalFilesPath(jsonConfig));
                    }
                    CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                        copyFolder(sourceDir, destDir, progressIndicator, index, length);
                        return true;
                    }, executor);
                    futures.add(future);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean perform(ProgressIndicator progressIndicator) {
        long startMs = System.currentTimeMillis();
        if (jsonConfig == null) {
            return false;
        }
        if (progressIndicator != null) {
            progressIndicator.setText2("Compute the dependence...");
        }

        if (bazelWorkspace != null && bazelWorkspace.isValid()) {
            if (progressIndicator != null) {
                String message = "Start synchronize bazel dependence files...";
                progressIndicator.setText2(message);
                consoleWindow.println(message, ConsoleViewContentType.NORMAL_OUTPUT);
                consoleWindow.println("dependence module: " + bazelWorkspace.getDependenceName().toString(),
                        ConsoleViewContentType.NORMAL_OUTPUT);
            }
            trySyncBazelDependenceDirectory(progressIndicator);
        } else {
            if (tafMakefileProperty != null) {
                List<String> jceIncludeFilePaths = tafMakefileProperty.getJceDependenceRecurseIncludes(
                        jsonConfig.getTafjceRemoteDirs(), false);
                // Add itself
                jceIncludeFilePaths.add(Constants.HOME_TAFJCE + "/" + app + "/" + target);
                jceIncludeFilePaths = CollectionUtil.uniq(jceIncludeFilePaths);
                Collections.sort(jceIncludeFilePaths);

                if (progressIndicator != null) {
                    String message = "Start synchronize dependence files...";
                    progressIndicator.setText2(message);
                    consoleWindow.println(message, ConsoleViewContentType.NORMAL_OUTPUT);
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
                trySyncTafjceDependenceDirectory(progressIndicator, length);
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        consoleWindow.println("", ConsoleViewContentType.NORMAL_OUTPUT);
        consoleWindow.println("TAF synchronize file finished. Cost " +
                        (System.currentTimeMillis() - startMs) + " milliseconds, Ignore file " +
                ignoreFileCount + ", Sync file " + syncFileCount + ".",
                ConsoleViewContentType.ERROR_OUTPUT);
        executor.shutdown();
        return true;
    }
}
