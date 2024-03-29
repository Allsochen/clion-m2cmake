package com.github.allsochen.m2cmake.generator;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.dependence.SambaFileSynchronizeWorker;
import com.github.allsochen.m2cmake.makefile.BazelWorkspace;
import com.github.allsochen.m2cmake.utils.FilterUtil;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.github.allsochen.m2cmake.utils.ProjectWrapper;
import com.github.allsochen.m2cmake.view.ConsoleWindow;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BazelToCmakeFileGenerator extends AbstractCmakeFileGenerator {

    private BazelWorkspace bazelWorkspace;
    private JsonConfig jsonConfig;
    private ConsoleWindow consoleWindow;
    private SambaFileSynchronizeWorker fsw;

    public BazelToCmakeFileGenerator(ProjectWrapper projectWrapper,
                                     BazelWorkspace bazelWorkspace,
                                     JsonConfig jsonConfig,
                                     ConsoleWindow consoleWindow,
                                     SambaFileSynchronizeWorker fsw) {
        super(projectWrapper);
        this.projectWrapper = projectWrapper;
        this.bazelWorkspace = bazelWorkspace;
        this.jsonConfig = jsonConfig;
        this.consoleWindow = consoleWindow;
        this.fsw = fsw;
    }

    public static File getCmakeListFile(String basePath) {
        return new File(basePath + File.separator + "CMakeLists.txt");
    }

    /**
     * Replace `\\` to `/`
     *
     * @param path
     * @return
     */
    private static String convertPathSeparator(String path) {
        return path.replaceAll("\\\\", "/");
    }

    private void writeHeader(BufferedWriter bw) throws IOException {
        // Write header.
        bw.write("# This file is generated by TAF/tRPC m2cmake plugin\n");
        bw.write("# http://www.github.com/allsochen/clion-m2cmake\n");
        bw.newLine();
        String cmakeVersion = "3.10";
        if (!jsonConfig.getCmakeVersion().isEmpty()) {
            cmakeVersion = jsonConfig.getCmakeVersion();
        }
        bw.write("cmake_minimum_required(VERSION " + cmakeVersion + ")");
        bw.newLine();

        bw.write("project(" + projectWrapper.getTarget() + ")");
        bw.newLine();
        bw.write("set(CMAKE_CXX_STANDARD 17)");
        bw.newLine();

        String cxxFlags = "-std=c++17 -Wno-narrowing -fno-strict-aliasing -Wno-deprecated-declarations -fPIC -Wno-deprecated -Wall";
        bw.write("set(CMAKE_CXX_FLAGS \"" + cxxFlags + "\")");
        bw.newLine();
    }

    public void writeConfigIncludes(BufferedWriter bw) throws IOException {
        bw.newLine();
        bw.write("# Configuration includes");
        bw.newLine();

        Set<String> configIncludes = new LinkedHashSet<>(this.jsonConfig.getIncludes());
        for (String include : configIncludes) {
            bw.write("include_directories(" + convertPathSeparator(include) + ")");
            bw.newLine();
        }
    }

    /**
     * Write source path of current project.
     * <p>
     * Path includes:
     * ${project}/.
     * ${project}/src
     * ${project}/../../src
     *
     * @param bw
     * @throws IOException
     */
    public void writeProjectIncludeOrSrcPath(BufferedWriter bw) throws IOException {
        // Add root directory.
        bw.newLine();
        bw.write("include_directories(./)");
        bw.newLine();

        // Add src or sub src directory from project.
        File root = new File(Objects.requireNonNull(this.projectWrapper.getProject().getBasePath()));
        Set<String> includes = new TreeSet<>();
        List<File> subDirectories = new ArrayList<>();
        walkToFilterIncludeOrSrcDir(root, false, subDirectories);
        for (File subDirectory : subDirectories) {
            includes.add(convertPathSeparator(subDirectory.getAbsolutePath()));
        }
        for (String include : includes) {
            bw.write("include_directories(" + include + ")");
            bw.newLine();
        }
    }

    public void writeLocalBazelBinBasePath(BufferedWriter bw) throws IOException {
        bw.newLine();
        bw.write("# Bazel bin includes");
        bw.newLine();
        String bazelBinFilesPath = ProjectUtil.getLocalBazelBinFilesPath(jsonConfig);
        bw.write("include_directories(" + convertPathSeparator(bazelBinFilesPath) + ")");
    }

    public void writeLocalBazelRepositoryBasePath(BufferedWriter bw) throws IOException {
        bw.newLine();
        bw.write("# Bazel repository includes");
        bw.newLine();
        String localBazelRepositoryDir = ProjectUtil.getLocalBazelRepositoryFilesPath(jsonConfig);
        bw.write("include_directories(" + convertPathSeparator(localBazelRepositoryDir) + ")");

    }

    public void writeLocalBazelBinExternalWorkspacePath(BufferedWriter bw) throws IOException {
        // Add itself dependence gen files.
        bw.newLine();
        File itself = ProjectUtil.getLocalBazelBinExternalWorkspaceFile(jsonConfig, bazelWorkspace.getTarget());
        bw.write("include_directories(" + convertPathSeparator(itself.getAbsolutePath()) + ")");
        bw.newLine();
    }

    public void writeLocalBazelBinExternalOrSubPath(
            BufferedWriter bw,
            List<File> remoteSyncChildrenDirectories,
            List<String> bazelWorkspaceDependenceNames) throws IOException {
        bw.newLine();
        File localBazelBinFilesExternal = ProjectUtil.getLocalBazelBinExternalFile(jsonConfig);
        File[] localBazelBinFilesExternals = localBazelBinFilesExternal.listFiles();
        writeBazelExternalDependence(bw, remoteSyncChildrenDirectories,
                bazelWorkspaceDependenceNames, localBazelBinFilesExternal,
                localBazelBinFilesExternals);
    }

    public void writeLocalBazelRepositoryExternalOrSubPath(
            BufferedWriter bw,
            List<File> remoteSyncChildrenDirectories,
            List<String> bazelWorkspaceDependenceNames) throws IOException {
        bw.newLine();
        File localBazelRepositoryFilesExternal = ProjectUtil.getLocalBazelRepositoryExternalFile(jsonConfig);
        File[] localBazelRepositoryFilesExternals = localBazelRepositoryFilesExternal.listFiles();
        writeBazelExternalDependence(bw, remoteSyncChildrenDirectories,
                bazelWorkspaceDependenceNames, localBazelRepositoryFilesExternal,
                localBazelRepositoryFilesExternals);

    }

    public void writeExecutable(BufferedWriter bw) throws IOException {
        bw.newLine();
        bw.write("file(GLOB_RECURSE CMAKE_FILES *.c *.cc *.cpp *.h)");
        bw.newLine();
        bw.write("add_executable(" + projectWrapper.getTarget() + " ${CMAKE_FILES})");
        bw.newLine();
    }

    @Override
    public void create() {
        Project project = projectWrapper.getProject();
        try {
            File cmakeFile = getCmakeListFile(project.getBasePath());
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(cmakeFile), StandardCharsets.UTF_8));
            writeHeader(bw);
            writeConfigIncludes(bw);
            writeProjectIncludeOrSrcPath(bw);
            writeLocalBazelBinBasePath(bw);
            List<File> remoteSyncSubDirectory = getRemoteSyncSubDirectory();
            List<String> bazelWorkspaceDependenceModuleNames = getBazelWorkspaceDependenceModuleNames();

            writeLocalBazelBinExternalWorkspacePath(bw);
            writeLocalBazelBinExternalOrSubPath(bw, remoteSyncSubDirectory, bazelWorkspaceDependenceModuleNames);

            writeLocalBazelRepositoryBasePath(bw);
            writeLocalBazelRepositoryExternalOrSubPath(bw, remoteSyncSubDirectory, bazelWorkspaceDependenceModuleNames);

            writeExecutable(bw);
            bw.flush();
            bw.close();
            cmakeFile.setLastModified(System.currentTimeMillis());
            consoleWindow.println("", ConsoleViewContentType.NORMAL_OUTPUT);
            consoleWindow.println("Transfer TAF/tRPC bazel to CMakeList finished.",
                    ConsoleViewContentType.ERROR_OUTPUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeBazelExternalDependence(BufferedWriter bw,
                                              List<File> remoteSyncChildrenDirectories,
                                              List<String> bazelWorkspaceDependenceNames,
                                              File localBazelBinOrRepositoryFilesExternal,
                                              File[] localBazelBinOrRepositoryFilesExternals) throws IOException {
        if (localBazelBinOrRepositoryFilesExternals != null) {
            Set<String> includes = new TreeSet<>();
            // 1.Local path must in bazel WORKSPACE file
            for (File file : localBazelBinOrRepositoryFilesExternals) {
                consoleWindow.println("externalPath: " + file.getAbsolutePath(),
                        ConsoleViewContentType.NORMAL_OUTPUT);
                if (file.isDirectory() && bazelWorkspaceDependenceNames.contains(file.getName())) {
                    includes.add(convertPathSeparator(file.getAbsolutePath()));
                }
                // Walk through the sub directory. try to add include/src directory.
                List<File> subDirectories = new ArrayList<>();
                walkToFilterIncludeOrSrcDir(file, true, subDirectories);
                for (File subDirectory : subDirectories) {
                    includes.add(convertPathSeparator(subDirectory.getAbsolutePath()));
                }
            }
            // 2.Add remote dependence path.
            for (File file : remoteSyncChildrenDirectories) {
                if (file.isDirectory()) {
                    // bazel-bin|repository/name
                    String dependence = localBazelBinOrRepositoryFilesExternal.getAbsolutePath() +
                            File.separator + file.getName();
                    includes.add(convertPathSeparator(dependence));
                }
            }
            for (String include : includes) {
                bw.write("include_directories(" + include + ")");
                bw.newLine();
            }
        } else {
            consoleWindow.println("bazel bin/repository external path is empty: " +
                            localBazelBinOrRepositoryFilesExternal.getAbsolutePath(),
                    ConsoleViewContentType.NORMAL_OUTPUT);
        }
    }

    private List<String> getBazelWorkspaceDependenceModuleNames() {
        // add default functional.
        bazelWorkspace.add(BazelWorkspace.defaultFunctionals());
        List<String> bazelWorkspaceDependenceNames = bazelWorkspace.getDependenceName();
        consoleWindow.println("bazelWorkspaceDependenceModule: " + bazelWorkspaceDependenceNames.toString(),
                ConsoleViewContentType.NORMAL_OUTPUT);
        return bazelWorkspaceDependenceNames;
    }

    private static void walkToFilterIncludeOrSrcDir(File file, boolean exclude, List<File> includeOrSrcDir) {
        if (file != null && file.isDirectory()) {
            if (FilterUtil.isIncludeOrSrc(file, exclude)) {
                includeOrSrcDir.add(file);
            } else {
                // Walk the sub directory while it is not an include/src directory.
                File[] files = file.listFiles();
                if (files == null) {
                    return;
                }
                for (File newFile : files) {
                    walkToFilterIncludeOrSrcDir(newFile, exclude, includeOrSrcDir);
                }
            }
        }
    }

    /**
     * ${project}/bazel-bin
     * ${project}/bazel-bin/xxx
     * ${project}/bazel-PROJECT/external
     * ${project}/bazel-PROJECT/external/xxx
     *
     * @return
     */
    private List<File> getRemoteSyncSubDirectory() {
        List<File> children = new ArrayList<>();
        List<File> syncDirs = fsw.getRemoteBazelSyncDir(true, true);
        for (File file : syncDirs) {
            if (file.isDirectory()) {
                // Add current directory
                children.add(file);
                File[] listFiles = file.listFiles();
                if (listFiles == null) {
                    continue;
                }
                for (File subFile : listFiles) {
                    if (file.isDirectory()) {
                        // Add sub-directory.
                        children.add(subFile);
                    }
                }
            }
        }
        return children;
    }

}
