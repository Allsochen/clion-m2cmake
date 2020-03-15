package com.github.allsochen.m2cmake.makefile;

import com.github.allsochen.m2cmake.build.AutomaticReloadCMakeBuilder;
import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.github.allsochen.m2cmake.view.ConsoleWindow;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BazelCmakeFileGenerator {
    private String app;
    private String target;
    private String basePath;
    private BazelWorkspace bazelWorkspace;
    private JsonConfig jsonConfig;
    private ConsoleWindow consoleWindow;

    public BazelCmakeFileGenerator(String app, String target, String basePath,
                                   BazelWorkspace bazelWorkspace,
                                   JsonConfig jsonConfig,
                                   ConsoleWindow consoleWindow) {
        this.app = app;
        this.target = target;
        this.basePath = basePath;
        this.bazelWorkspace = bazelWorkspace;
        this.jsonConfig = jsonConfig;
        this.consoleWindow = consoleWindow;
    }

    public static File getCmakeListFile(String basePath) {
        return new File(basePath + File.separator + "CMakeLists.txt");
    }

    private String transferPathSeperator(String path) {
        return path.replaceAll("\\\\", "/");
    }

    public void create() {
        try {
            File cmakeFile = getCmakeListFile(this.basePath);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(cmakeFile), StandardCharsets.UTF_8));
            // Write header.
            bw.write("# This file is generated by TAF m2cmake plugin\n");
            bw.write("# http://www.github.com/allsochen/clion-m2cmake\n");
            bw.newLine();
            String cmakeVersion = "3.10";
            if (!jsonConfig.getCmakeVersion().isEmpty()) {
                cmakeVersion = jsonConfig.getCmakeVersion();
            }
            bw.write("cmake_minimum_required(VERSION " + cmakeVersion + ")");
            bw.newLine();

            bw.write("project(" + target + ")");
            bw.newLine();
            bw.write("set(CMAKE_CXX_STANDARD 11)");
            bw.newLine();

            String cxxFlags = "-std=c++11 -Wno-narrowing -fno-strict-aliasing -Wno-deprecated-declarations -fPIC -Wno-deprecated -Wall";
            bw.write("set(CMAKE_CXX_FLAGS \"" + cxxFlags + "\")");
            bw.newLine();

            bw.newLine();
            bw.write("#配置include");
            bw.newLine();

            Set<String> configIncludes = new LinkedHashSet<>(this.jsonConfig.getIncludes());
            for (String include : configIncludes) {
                bw.write("include_directories(" + include + ")");
                bw.newLine();
            }

            bw.newLine();
            bw.write("include_directories(./)");
            bw.newLine();
            bw.newLine();

            // add default functional.
            bazelWorkspace.add(BazelWorkspace.defaultFunctionals());
            List<String> bazelDependenceNames = bazelWorkspace.getDependenceName();
            consoleWindow.println("bazelDependenceName: " + bazelDependenceNames.toString(),
                    ConsoleViewContentType.NORMAL_OUTPUT);
            bw.newLine();
            bw.write("#bazel genfile includes");
            bw.newLine();
            String bazelGenFileDir = ProjectUtil.getBazelGenFilesPath(jsonConfig);
            bw.write("include_directories(" + transferPathSeperator(bazelGenFileDir) + ")");
            bw.newLine();
            // Add itself dependence gen files.
            File itself = ProjectUtil.getBazelGenFilesExternalWorkspaceFile(jsonConfig, bazelWorkspace.getTarget());
            bw.write("include_directories(" + transferPathSeperator(itself.getAbsolutePath()) + ")");
            bw.newLine();
            File bazelGenFileExternal = ProjectUtil.getBazelGenFilesExternalFile(jsonConfig);
            File[] bazelGenFileExternals = bazelGenFileExternal.listFiles();
            if (bazelGenFileExternals != null) {
                for (File file : bazelGenFileExternals) {
                    consoleWindow.println("bazelGenFileExternals name: " + file.getName(),
                            ConsoleViewContentType.NORMAL_OUTPUT);
                    if (file.isDirectory() && bazelDependenceNames.contains(file.getName())) {
                        bw.write("include_directories(" + transferPathSeperator(file.getAbsolutePath()) + ")");
                        bw.newLine();
                    }
                }
            } else {
                consoleWindow.println("bazel gen file external path is empty: " +
                                bazelGenFileExternal.getAbsolutePath(),
                        ConsoleViewContentType.LOG_WARNING_OUTPUT);
            }
            bw.newLine();

            bw.write("#bazel git repository includes");
            bw.newLine();
            String bazelRepositoryDir = ProjectUtil.getBazelRepositoryFilesPath(jsonConfig);
            bw.write("include_directories(" + transferPathSeperator(bazelRepositoryDir) + ")");
            bw.newLine();
            File bazelRepositoryFilesExternal = ProjectUtil.getBazelRepositoryExternalFile(jsonConfig);
            File[] bazelRepositoryFilesExternals = bazelRepositoryFilesExternal.listFiles();
            if (bazelRepositoryFilesExternals != null) {
                for (File file : bazelRepositoryFilesExternals) {
                    consoleWindow.println("bazelRepositoryFilesExternals name: " + file.getName(),
                            ConsoleViewContentType.NORMAL_OUTPUT);
                    if (file.isDirectory() && bazelDependenceNames.contains(file.getName())) {
                        bw.write("include_directories(" + transferPathSeperator(file.getAbsolutePath()) + ")");
                        bw.newLine();
                    }
                    // try to add include/src directory.
                    File[] subFiles = file.listFiles();
                    if (subFiles != null) {
                        for (File subFile : subFiles) {
                            if (subFile.isDirectory() && isIncludeOrSrc(subFile)) {
                                bw.write("include_directories(" +
                                        transferPathSeperator(subFile.getAbsolutePath()) + ")");
                                bw.newLine();
                            }
                        }
                    }
                }
            } else {
                consoleWindow.println("bazel repository external path is empty: " +
                                bazelRepositoryFilesExternal.getAbsolutePath(),
                        ConsoleViewContentType.LOG_WARNING_OUTPUT);
            }
            bw.newLine();

            bw.write("file(GLOB_RECURSE CMAKE_FILES *.cpp *.h)");
            bw.newLine();
            bw.write("add_executable(" + target + " ${CMAKE_FILES})");
            bw.newLine();
            bw.flush();
            bw.close();
            cmakeFile.setLastModified(System.currentTimeMillis());
            consoleWindow.println("", ConsoleViewContentType.NORMAL_OUTPUT);
            consoleWindow.println("Transfer TAF bazel to CMakeList finished.",
                    ConsoleViewContentType.ERROR_OUTPUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isIncludeOrSrc(File file) {
        String fileName = file.getName();
        if (fileName.equals("include") ||
                fileName.equals("src")) {
            return true;
        }
        return false;
    }

    public void open(Project project) {
        try {
            VirtualFile[] virtualFiles = FileEditorManager.getInstance(project).getOpenFiles();
            for (VirtualFile virtualFile : virtualFiles) {
                if (virtualFile.getName().contains("CMakeLists")) {
                    virtualFile.refresh(false, false);
                }
            }
            File cmakeFile = BazelCmakeFileGenerator.getCmakeListFile(basePath);

            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(cmakeFile);
            if (vf != null) {
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, vf);
                FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        try {
            // Set project to auto build.
            if (jsonConfig.isAutomaticReloadCMake()) {
                try {
                    LocalFileSystem.getInstance().refresh(true);
                    AutomaticReloadCMakeBuilder.build(basePath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
