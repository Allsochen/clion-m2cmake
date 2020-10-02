package com.github.allsochen.m2cmake.makefile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BazelWorkspaceAnalyser {

    public static BazelWorkspace analysis(String basePath, String projectName) {
        BazelWorkspace mergedBazelWorkspace = new BazelWorkspace();
        File folder = new File(basePath);

        List<File> files = new ArrayList<>();
        walk(folder, files);
        for (File file : files) {
            try {
                BazelWorkspace bazelWorkspace = parse(file);
                mergedBazelWorkspace.merge(bazelWorkspace);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        boolean foundWorkspace = false;
        for (BazelFunctional bazelFunctional : mergedBazelWorkspace.getFunctionals()) {
            if (bazelFunctional.getType() == BazelFunctionalType.WORKSPACE) {
                foundWorkspace = true;
                break;
            }
        }
        // Add default workspace.
        if (!foundWorkspace) {
            mergedBazelWorkspace.add(new BazelFunctional(BazelFunctionalType.WORKSPACE, projectName));
        }
        return mergedBazelWorkspace;
    }

    private static void walk(File file, List<File> makefiles) {
        if (file.isFile()) {
            if (isBazelFamily(file)) {
                makefiles.add(file);
            }
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File newFile : files) {
                walk(newFile, makefiles);
            }
        }
    }

    private static boolean isBazelFamily(File file) {
        return file.getName().toUpperCase().contains("WORKSPACE");
    }

    private static BazelWorkspace parse(File file) throws IOException {
        BazelWorkspace bazelWorkspace = new BazelWorkspace();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String line;
        BazelFunctionalType functionalType = BazelFunctionalType.UNKNOWN;
        boolean isFunctionalStart = false;
        boolean isFunctionalEnd = false;
        BazelFunctional bazelFunctional = null;
        while ((line = bufferedReader.readLine()) != null) {
            line = line.replaceAll(" ", "");
            // handle with workspace name.
            // workspace(name="FeatureAPI")
            if (line.startsWith("workspace(name=")) {
                BazelFunctional workspace = new BazelFunctional();
                workspace.setType(BazelFunctionalType.WORKSPACE);

                String name = line.replace("workspace(name=\"", "")
                        .replace("\"", "")
                        .replace(",", "")
                        .replace(")", "");
                workspace.setName(name);
                bazelWorkspace.add(workspace);
            }

            // handle with git_repository
            // git_repository(
            //         name = "AServer",
            //         remote = "http://xxx.com/AServer.git",
            //         branch = "master"
            // )
            if (line.startsWith("git_repository(")) {
                // functional begin.
                functionalType = BazelFunctionalType.GIT_REPOSITORY;
                isFunctionalStart = true;
                isFunctionalEnd = false;
            } else if (line.startsWith("http_archive(")) {
                // functional begin.
                functionalType = BazelFunctionalType.HTTP_ARCHIVE;
                isFunctionalStart = true;
                isFunctionalEnd = false;
            } else if (line.startsWith(")")) {
                // functional end
                functionalType = BazelFunctionalType.UNKNOWN;
                isFunctionalStart = false;
                isFunctionalEnd = true;

                bazelWorkspace.add(bazelFunctional);
            } else {
                if (isFunctionalStart && !isFunctionalEnd) {
                    if (line.startsWith("name=")) {
                        String name = line.replace("name=", "")
                                .replaceAll("\"", "")
                                .replace(",", "");
                        bazelFunctional = new BazelFunctional();
                        bazelFunctional.setType(functionalType);
                        bazelFunctional.setName(name);
                    } else {
                        // ignore options.
                    }
                }
            }
        }
        bufferedReader.close();
        return bazelWorkspace;
    }
}
