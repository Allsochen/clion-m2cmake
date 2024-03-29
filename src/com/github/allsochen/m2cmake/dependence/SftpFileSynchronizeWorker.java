package com.github.allsochen.m2cmake.dependence;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.configuration.JsonConfigBuilder;
import com.github.allsochen.m2cmake.constants.Constants;
import com.github.allsochen.m2cmake.makefile.BazelWorkspace;
import com.github.allsochen.m2cmake.utils.FileUtils;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.github.allsochen.m2cmake.utils.ProjectWrapper;
import com.github.allsochen.m2cmake.view.ConsoleWindow;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SftpFileSynchronizeWorker {

    private JsonConfig jsonConfig;
    private BazelWorkspace bazelWorkspace;

    private ProjectWrapper projectWrapper;

    private ConsoleWindow consoleWindow;

    private ExecutorService executor;
    private List<CompletableFuture<Boolean>> scanFutures = new ArrayList<>();
    private List<CompletableFuture<Boolean>> copyFutures = new ArrayList<>();
    private AtomicInteger lastTaskIndex = new AtomicInteger(0);
    private AtomicInteger totalTask = new AtomicInteger(0);
    private AtomicInteger ignoreFileCount = new AtomicInteger(0);
    private AtomicInteger unmodifiedFileCount = new AtomicInteger(0);
    private AtomicInteger syncFileCount = new AtomicInteger(0);

    private List<CopyTask> copyTasks = new ArrayList<>();

    public static class CopyTask {
        public File source;
        public File destination;

        public CopyTask(File source, File destination) {
            this.source = source;
            this.destination = destination;
        }
    }

    public SftpFileSynchronizeWorker(JsonConfig jsonConfig,
                                     BazelWorkspace bazelWorkspace,
                                     ProjectWrapper projectWrapper) {
        this.jsonConfig = jsonConfig;
        this.bazelWorkspace = bazelWorkspace;
        this.projectWrapper = projectWrapper;
        this.consoleWindow = ConsoleWindow.getInstance(projectWrapper.getProject());
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
        if ("h".equals(suffix) ||
                "cpp".equals(suffix) ||
                "hpp".equals(suffix) ||
                "cc".equals(suffix) ||
                "jce".equals(suffix) ||
                "log".equals(suffix) ||
                "mk".equals(suffix) ||
                "bzl".equals(suffix) ||
                "md".equals(suffix) ||
                "inc".equals(suffix) ||
                "proto".equals(suffix) ||
                "yml".equals(suffix)) {
            return true;
        } else {
            String name = fileName.toUpperCase();
            return name.contains("WORKSPACE") || name.contains("BUILD");
        }
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
        return name.startsWith(".") ||
                name.endsWith(".runfiles") ||
                "_objs".equals(name) ||
                "bazel-out".equals(name);
    }

    private void scanAndCopyFiles(File source, File destination,
                                  ProgressIndicator progressIndicator) {
        try {
            progressIndicator.checkCanceled();
        } catch (ProcessCanceledException e) {
            consoleWindow.println("Synchronized canceled.", ConsoleViewContentType.ERROR_OUTPUT);
            return;
        }
        if (!source.exists()) {
            return;
        }
        if (isIgnore(source)) {
            consoleWindow.println("IGNORE\t" + source.getPath(),
                    ConsoleViewContentType.NORMAL_OUTPUT);
            return;
        }

        // Change the file in bazel-genfile directory to bazel-genfiles/external/bazelWorkspace/ directory.
        // /xxx/bazel-genfile/A.cpp ==> /yyy/bazel-genfile/external/bazelWorkspace/A.cpp
        // Parent directory: workspace/bazel-bin
        if (bazelWorkspace != null && bazelWorkspace.isValid() &&
                source.getParentFile().getName().equals(Constants.BAZEL_BIN) && source.isFile()) {
            File bazelBinExternalWorkspaceDir = ProjectUtil
                    .getLocalBazelBinExternalWorkspaceFile(jsonConfig, bazelWorkspace.getTarget());
            if (!bazelBinExternalWorkspaceDir.exists()) {
                bazelBinExternalWorkspaceDir.mkdirs();
            }
            destination = new File(bazelBinExternalWorkspaceDir, source.getName());
        }

        if (source.isDirectory()) {
            // no force synchronize with base dependence.
            if (bazelWorkspace != null &&
                    jsonConfig.getNoForceSyncModules().contains(source.getName()) &&
                    destination.exists()) {
                consoleWindow.println("EXIST\t" + source.getPath(),
                        ConsoleViewContentType.ERROR_OUTPUT);
                return;
            }

            if (bazelWorkspace != null && isUnderBazelProjectDirectory(source)) {
                consoleWindow.println("EXIST\t" + source.getPath(),
                        ConsoleViewContentType.ERROR_OUTPUT);
                return;
            }

            if (!destination.exists()) {
                destination.mkdirs();
                consoleWindow.println("CREATE\t" + destination.getPath(),
                        ConsoleViewContentType.NORMAL_OUTPUT);
            }
            String[] files = source.list();
            if (files == null) {
                return;
            }
            List<String> priorityFiles = setFilesPriority(files);
            for (String fileName : priorityFiles) {
                StringBuilder message = new StringBuilder();
                StringBuilder message2 = new StringBuilder();
                String operator = "IGNORE";
                File srcFile = new File(source, fileName);
                File destFile = new File(destination, fileName);
                if ((isCopyFileFamily(fileName) && hasDiff(srcFile, destFile)) || srcFile.isDirectory()) {
                    operator = "SCAN";
                    scanAndCopyFiles(srcFile, destFile, progressIndicator);
                } else {
                    if ((isCopyFileFamily(fileName) && !hasDiff(srcFile, destFile))) {
                        operator = "UNMODIFIED";
                        unmodifiedFileCount.incrementAndGet();
                    } else {
                        ignoreFileCount.incrementAndGet();
                    }
                }
                message.append(operator).append(" ").append(srcFile.getPath());
                message2.append(operator).append("\t")
                        .append(srcFile.getPath()).append(" ===> ").append(destination.getPath());
                progressIndicator.setText("dependence synchronize...(" +
                        lastTaskIndex.intValue() + "/" + totalTask.intValue() + ")");
                progressIndicator.setText2(message.toString());
                if ("IGNORE".equals(operator)) {
                    consoleWindow.println(message2.toString(), ConsoleViewContentType.NORMAL_OUTPUT);
                } else {
                    consoleWindow.println(message2.toString(), ConsoleViewContentType.NORMAL_OUTPUT);
                }
            }
        } else {
            totalTask.incrementAndGet();
            copySingleFile(source, destination, progressIndicator);
        }
    }

    private List<String> setFilesPriority(String[] files) {
        List<String> priorityFiles = new LinkedList<>();
        List<String> modules = JsonConfigBuilder.defaultNoForceSyncModules();
        // push no force sync module to last.
        for (String file : files) {
            if (modules.contains(file)) {
                // add to last.
                priorityFiles.add(file);
            } else {
                // add to front.
                priorityFiles.add(0, file);
            }
        }
        return priorityFiles;
    }

    private void copySingleFile(File source, File destination, ProgressIndicator progressIndicator) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                progressIndicator.checkCanceled();
            } catch (ProcessCanceledException e) {
                consoleWindow.println("Synchronized canceled.", ConsoleViewContentType.ERROR_OUTPUT);
                return false;
            }
            try {
                return FileUtils.copyFileIfModified(source, destination);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }, executor).thenApply(result -> {
            StringBuilder message = new StringBuilder();
            StringBuilder message2 = new StringBuilder();
            String operator;
            if (result) {
                operator = "SYNC";
                syncFileCount.incrementAndGet();
            } else {
                operator = "UNMODIFIED";
                unmodifiedFileCount.incrementAndGet();
            }
            message.append(operator).append(" ").append(source.getPath());
            message2.append(operator).append("\t").append(source.getPath())
                    .append(" ===> ").append(destination.getPath());
            int index = lastTaskIndex.incrementAndGet();
            progressIndicator.setText("dependence synchronize...(" +
                    index + "/" + totalTask.intValue() + ")");
            progressIndicator.setText2(message.toString());
            consoleWindow.println(message2.toString(), ConsoleViewContentType.NORMAL_OUTPUT);
            return true;
        });
        copyFutures.add(future);
    }

    private void trySyncTafjceDependenceDirectory(ProgressIndicator progressIndicator) {
        Project project = projectWrapper.getProject();
        String target = projectWrapper.getTarget();
        try {
            List<String> paths = WebServersParser.parse(project.getBasePath());
            for (String path : paths) {
                consoleWindow.println("find 'tafjcedepend' directory in: " + path,
                        ConsoleViewContentType.NORMAL_OUTPUT);
                Stream<Path> walk = Files.walk(Paths.get(path));
                List<Path> pathList = walk.filter(path1 -> path1.getFileName().endsWith(Constants.TAFJCE_DEPEND))
                        .collect(Collectors.toList());
                for (Path tafJceDependencePath : pathList) {
                    File sourceDir = tafJceDependencePath.toFile();
                    File destDir = new File(ProjectUtil.getLocalTafjceDependenceDir(jsonConfig, target));
                    consoleWindow.println("Try async. source directory: " +
                                    sourceDir.getAbsolutePath() +
                                    " dest directory: " +
                                    destDir.getAbsolutePath(),
                            ConsoleViewContentType.NORMAL_OUTPUT);
                    CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                        scanAndCopyFiles(sourceDir, destDir, progressIndicator);
                        return true;
                    }, executor);
                    scanFutures.add(future);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveBazelBinFileToFront(List<File> files) {
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (file.getName().equals(Constants.BAZEL_BIN) && i != 0) {
                File removed = files.remove(i);
                files.add(0, removed);
            }
        }
    }

    private boolean isBazelProjectDirectory(String directoryName) {
        return ProjectUtil.isBazelProjectDirectory(directoryName, projectWrapper.getProject().getName())
                || ProjectUtil.isBazelProjectDirectory(directoryName, projectWrapper.getTarget())
                || ProjectUtil.isBazelProjectDirectory(directoryName, bazelWorkspace.getTarget());
    }

    private boolean isUnderBazelProjectDirectory(File file) {
        String name = "bazel-" + bazelWorkspace.getTarget() +
                File.separator + "external" +
                File.separator + bazelWorkspace.getTarget();
        name = name.toLowerCase();
        return file.getAbsolutePath().toLowerCase().endsWith(name);
    }

    /**
     * Get remote bazel synchronized directory. directory as followed:
     * <p>
     * ${project}/bazel-bin
     * ${project}/bazel-PROJECT/external
     *
     * @return
     */
    public List<File> getRemoteBazelSyncDirectory() {
        Project project = projectWrapper.getProject();
        List<File> targetFiles = new LinkedList<>();
        List<String> syncPaths = WebServersParser.parse(project.getBasePath());
        for (String syncPath : syncPaths) {
            consoleWindow.println("sync remote path: " + syncPath,
                    ConsoleViewContentType.NORMAL_OUTPUT);
            File rootFile = new File(syncPath);
            if (!rootFile.exists() || !rootFile.isDirectory()) {
                consoleWindow.println("WARMING: remote synchronize path is empty. " +
                                rootFile.getAbsolutePath(),
                        ConsoleViewContentType.ERROR_OUTPUT);
                continue;
            }
            File[] files = rootFile.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                String fileName = file.getName();
                File[] subFiles = file.listFiles();
                if (fileName.equals(Constants.BAZEL_BIN)) {
                    targetFiles.add(file);
                } else {
                    // Add bazel-workspaceName/external directory.
                    if (isBazelProjectDirectory(file.getName()) && subFiles != null) {
                        for (File subFile : subFiles) {
                            if (subFile.isDirectory() && "external".equals(subFile.getName())) {
                                targetFiles.add(subFile);
                            }
                        }
                    }
                }
            }
            // Synchronized bazel-bin directory first.
            moveBazelBinFileToFront(targetFiles);

            if (targetFiles.isEmpty()) {
                consoleWindow.println("WARMING: Not found bazel-genfiles or bazel-" +
                                bazelWorkspace.getTarget() + " directory. " +
                                "Please execute `bazel build ...` command on remote server " +
                                "before synchronize dependence.",
                        ConsoleViewContentType.ERROR_OUTPUT);
            }
        }
        return targetFiles;
    }

    private void trySyncBazelDependenceDirectory(ProgressIndicator progressIndicator) {
        try {
            List<File> targetFiles = getRemoteBazelSyncDirectory();
            for (int i = 0; i < targetFiles.size(); i++) {
                File sourceDir = targetFiles.get(i);
                int index = i + 1;
                // jce gen files
                File destDir;
                if (sourceDir.getName().equals(Constants.BAZEL_BIN)) {
                    destDir = new File(ProjectUtil.getLocalBazelBinFilesPath(jsonConfig));
                } else {
                    destDir = new File(ProjectUtil.getLocalBazelRepositoryExternalFilesPath(jsonConfig));
                }
                consoleWindow.println("Try async. source directory: " + sourceDir.getAbsolutePath() +
                                " dest directory: " + destDir.getAbsolutePath(),
                        ConsoleViewContentType.NORMAL_OUTPUT);
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    scanAndCopyFiles(sourceDir, destDir, progressIndicator);
                    return true;
                }, executor);
                scanFutures.add(future);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean perform(ProgressIndicator progressIndicator) {
        String app = projectWrapper.getApp();
        String target = projectWrapper.getTarget();
        long startMs = System.currentTimeMillis();
        progressIndicator.setText2("Compute the dependence...");

        if (bazelWorkspace != null && bazelWorkspace.isValid()) {
            String message = "Start synchronize bazel dependence files...";
            progressIndicator.setText2(message);
            consoleWindow.println(message, ConsoleViewContentType.NORMAL_OUTPUT);
            consoleWindow.println("dependence modules: " + bazelWorkspace.getDependenceName().toString(),
                    ConsoleViewContentType.NORMAL_OUTPUT);
            consoleWindow.println("no force sync modules: " + jsonConfig.getNoForceSyncModules(),
                    ConsoleViewContentType.ERROR_OUTPUT);
            trySyncBazelDependenceDirectory(progressIndicator);
        }

        CompletableFuture.allOf(copyFutures.toArray(new CompletableFuture[0])).join();
        long cost = (System.currentTimeMillis() - startMs) / 1000;
        consoleWindow.println("", ConsoleViewContentType.NORMAL_OUTPUT);
        consoleWindow.println("synchronize file finished. Cost " + cost + "s, Ignore file " +
                        ignoreFileCount + ", Unmodified file " +
                        unmodifiedFileCount + ", Sync file " + syncFileCount + ".",
                ConsoleViewContentType.ERROR_OUTPUT);
        executor.shutdown();
        return true;
    }
}
