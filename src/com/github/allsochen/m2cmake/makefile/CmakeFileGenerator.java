package com.github.allsochen.m2cmake.makefile;

import com.github.allsochen.m2cmake.build.AutomaticReloadCMakeBuilder;
import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.utils.CollectionUtil;
import com.github.allsochen.m2cmake.utils.Constants;
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
import java.util.*;

public class CmakeFileGenerator {
    private String app;
    private String target;
    private String basePath;
    private TafMakefileProperty tafMakefileProperty;
    private JsonConfig jsonConfig;
    private ConsoleWindow consoleWindow;

    public CmakeFileGenerator(String app, String target, String basePath,
                              TafMakefileProperty tafMakefileProperty,
                              JsonConfig jsonConfig,
                              ConsoleWindow consoleWindow) {
        this.app = app;
        this.target = target;
        this.basePath = basePath;
        this.tafMakefileProperty = tafMakefileProperty;
        this.jsonConfig = jsonConfig;
        this.consoleWindow = consoleWindow;
    }

    /**
     * Filter the ../.. path and transfer to the real path.
     *
     * @param includePath
     * @return
     */
    private String transferIncludePath(String includePath) {
        if (!includePath.matches(".*[a-zA-z].*")) {
            return "";
        }
        return this.tafMakefileProperty.transferMapping(includePath, jsonConfig.getDirMappings());
    }

    public static File getCmakeListFile(String basePath) {
        return new File(basePath + File.separator + "CMakeLists.txt");
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
            bw.write("set(CMAKE_CXX_STANDARD 17)");
            bw.newLine();

            String cxxFlags = "-std=c++17 -Wno-narrowing -fno-strict-aliasing -Wno-deprecated-declarations -fPIC -Wno-deprecated -Wall";
            bw.write("set(CMAKE_CXX_FLAGS \"" + cxxFlags + "\")");
            bw.newLine();

            bw.newLine();
            bw.write("#配置include");
            bw.newLine();

            Set<String> configIncludes = new LinkedHashSet<>(this.jsonConfig.getIncludes());
            if (configIncludes != null) {
                for (String include : configIncludes) {
                    bw.write("include_directories(" + include + ")");
                    bw.newLine();
                }
            }

            bw.newLine();
            bw.write("#服务include");
            bw.newLine();
            Set<String> includes = new LinkedHashSet<>(this.tafMakefileProperty.getIncludes());
            for (String include : includes) {
                include = transferIncludePath(include);
                if (include != null && !include.isEmpty()) {
                    bw.write("include_directories(" + transferIncludePath(include) + ")");
                    bw.newLine();
                }
            }
            bw.newLine();

            bw.write("#服务jce依赖");
            bw.newLine();
            bw.write("include_directories(./)");
            bw.newLine();
            String tafJceDepend = ProjectUtil.getTafjceDependenceDir(jsonConfig, target)
                    .replaceAll("\\\\", "/");
            bw.write("include_directories(" + tafJceDepend + ")");
            bw.newLine();

            List<String> localDir = new ArrayList<>();
            localDir.add(jsonConfig.getTafjceLocalDir());
            List<String> jceIncludeFilePaths = this.tafMakefileProperty.getJceDependenceRecurseIncludes(
                    localDir, true);
            // Add itself
            jceIncludeFilePaths.add(Constants.HOME_TAFJCE + "/" + app + "/" + target);
            jceIncludeFilePaths = CollectionUtil.uniq(jceIncludeFilePaths);
            Collections.sort(jceIncludeFilePaths);
            for (String include : jceIncludeFilePaths) {
                if (include != null && !include.isEmpty()) {
                    String dependenceLocalIncludePath = tafMakefileProperty.toLocalIncludePath(include,
                            jsonConfig.getDirMappings());
                    bw.write("include_directories(" + dependenceLocalIncludePath + ")");
                    bw.newLine();
                }
            }
            bw.newLine();

            bw.write("file(GLOB_RECURSE CMAKE_FILES *.c *.cc *.cpp *.h)");
            bw.newLine();
            bw.write("add_executable(" + target + " ${CMAKE_FILES})");
            bw.newLine();
            bw.flush();
            bw.close();
            cmakeFile.setLastModified(System.currentTimeMillis());
            consoleWindow.println("", ConsoleViewContentType.NORMAL_OUTPUT);
            consoleWindow.println("Transfer TAF makefile to CMakeList finished.",
                    ConsoleViewContentType.ERROR_OUTPUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void open(Project project) {
        try {
            VirtualFile[] virtualFiles = FileEditorManager.getInstance(project).getOpenFiles();
            for (VirtualFile virtualFile : virtualFiles) {
                if (virtualFile.getName().contains("CMakeLists")) {
                    virtualFile.refresh(false, false);
                }
            }
            File cmakeFile = CmakeFileGenerator.getCmakeListFile(basePath);

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
