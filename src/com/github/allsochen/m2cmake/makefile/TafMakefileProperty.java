package com.github.allsochen.m2cmake.makefile;

import com.github.allsochen.m2cmake.utils.Constants;

import java.io.File;
import java.util.*;

public class TafMakefileProperty {
    private String app = "";
    private List<String> targets = new ArrayList<>();
    private String cxxFlags = "";
    private List<String> includes = new ArrayList<>();
    private List<String> jceIncludes = new ArrayList<>();

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public String getCxxFlags() {
        return cxxFlags;
    }

    public void setCxxFlags(String cxxFlags) {
        this.cxxFlags = cxxFlags;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public List<String> getJceIncludes() {
        return jceIncludes;
    }

    public void setJceIncludes(List<String> jceIncludes) {
        this.jceIncludes = jceIncludes;
    }

    public void addTargets(String target) {
        this.targets.add(target);
    }

    public void addTargets(List<String> targets) {
        this.targets.addAll(targets);
    }

    public void addIncludes(List<String> includes) {
        this.includes.addAll(includes);
    }

    public void addJceIncludes(List<String> jceIncludes) {
        this.jceIncludes.addAll(jceIncludes);
    }

    /**
     * point to source path by specified tafjce source directory.
     * /home/tafjce/MTT/AServer/AServer.mk => Z:/tafjce/MTT/AServer/AServer.mk
     *
     * @param includePath
     * @param tafjceRemoteDirs
     * @return
     */
    private static List<String> toTafjceRemoteOrLocalMappingIncludeFilePath(String includePath,
                                                                            List<String> tafjceRemoteDirs) {
        List<String> tafjceMappingRemoteFilePath = new ArrayList<>();
        // tafjceSourceDir => Z:/tafjce
        for (String tafjceRemoteDir : tafjceRemoteDirs) {
            tafjceMappingRemoteFilePath.add(includePath.replace(Constants.HOME_TAFJCE, tafjceRemoteDir));
        }
        return tafjceMappingRemoteFilePath;
    }

    /**
     * Remove the `include` and `/xxx.mk` fragment.
     * include /home/tafjce/MTT/AServer/AServer.mk => /home/tafjce/MTT/AServer
     *
     * @param jceIncludePath
     * @return
     */
    public static String toIncludePath(String jceIncludePath) {
        String newPath = toRealFilePath(jceIncludePath);
        if (jceIncludePath.endsWith(".mk")) {
            newPath = newPath.substring(0, newPath.lastIndexOf("/"));
        }
        return newPath;
    }


    /**
     * Remove the `include` and `/xxx.mk` fragment.
     * /home/tafjce/MTT/AServer/AServer.mk => D:/Codes/tafjce/MTT/AServer
     *
     * @param jceIncludePath
     * @return
     */
    public String toLocalIncludePath(String jceIncludePath, Map<String, String> dirMappings) {
        String path = toIncludePath(jceIncludePath);
        return transferMapping(path, dirMappings);
    }

    public String transferMapping(String path, Map<String, String> dirMappings) {
        Iterator<Map.Entry<String, String>> iterator = dirMappings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (path.contains(entry.getKey())) {
                return path.replace(entry.getKey(), entry.getValue());
            }
        }
        return path;
    }

    /**
     * Remove the `include` prefix.
     * include /home/tafjce/MTT/AServer/Aserver.mk => /home/tafjce/MTT/AServer/AServer.mk
     *
     * @param includePath
     * @return
     */
    public static String toRealFilePath(String includePath) {
        String newPath = includePath;
        if (newPath.startsWith("include ")) {
            newPath = newPath.replace("include ", "").trim();
        }
        return newPath;
    }

    public List<String> getJceDependenceRecurseIncludes(List<String> tafjceRemoteOrLocalDirs, boolean recurse) {
        // Cached the dependence includes.
        Set<String> uniqDependenceIncludes = new LinkedHashSet<>();
        Set<String> uniqFilePaths = new HashSet<>();
        for (String jceInclude : jceIncludes) {
            if (recurse) {
                // Try to analysis from the mk file.
                // Only analysis the first floor to void death circle.
                List<String> filePaths = toTafjceRemoteOrLocalMappingIncludeFilePath(toRealFilePath(jceInclude),
                        tafjceRemoteOrLocalDirs);
                for (String filePath : filePaths) {
                    File file = new File(filePath);
                    if (file.exists() && !uniqFilePaths.contains(file.getPath())) {
                        try {
                            // it's dependence.
                            TafMakefileProperty tafMakefileProperty = TafMakefileAnalyser.extractInclude(file);
                            tafMakefileProperty.getIncludes().forEach(referenceInclude -> {
                                referenceInclude = toIncludePath(referenceInclude);
                                if (referenceInclude != null && !referenceInclude.isEmpty()) {
                                    uniqDependenceIncludes.add(referenceInclude);
                                }
                            });
                            // itself
                            tafMakefileProperty.getJceIncludes().forEach(referenceJceInclude -> {
                                referenceJceInclude = toIncludePath(referenceJceInclude);
                                if (referenceJceInclude != null && !referenceJceInclude.isEmpty()) {
                                    uniqDependenceIncludes.add(referenceJceInclude);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        uniqFilePaths.add(file.getPath());
                        // Only read dependence file once.
                        break;
                    }
                }
            }
            uniqDependenceIncludes.add(toIncludePath(jceInclude));
        }
        List<String> includes = new ArrayList<>(uniqDependenceIncludes);
        Collections.sort(includes);
        return includes;
    }
}
