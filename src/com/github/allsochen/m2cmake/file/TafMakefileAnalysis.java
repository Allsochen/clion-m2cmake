package com.github.allsochen.m2cmake.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class TafMakefileAnalysis {

    public TafMakefileProperty analysis(String basePath) {
        TafMakefileProperty totalTmp = new TafMakefileProperty();
        File folder = new File(basePath);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (isMakefileFamily(file)) {
                try {
                    TafMakefileProperty tmp = extractIncludeDirectory(file);
                    if (tmp.getApp() != null && !tmp.getApp().isEmpty()) {
                        totalTmp.setApp(tmp.getApp());
                    }
                    if (tmp.getTarget() != null && !tmp.getTarget().isEmpty()) {
                        totalTmp.setTarget(tmp.getTarget());
                    }
                    if (tmp.getCxxFlags() != null && !tmp.getCxxFlags().isEmpty()) {
                        totalTmp.setCxxFlags(tmp.getCxxFlags());
                    }
                    if (tmp.getIncludes() != null && !tmp.getIncludes().isEmpty()) {
                        totalTmp.addIncludes(tmp.getIncludes());
                    }
                    if (tmp.getJceIncludes() != null && !tmp.getJceIncludes().isEmpty()) {
                        totalTmp.addJceIncludes(tmp.getJceIncludes());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return totalTmp;
    }

    private boolean isMakefileFamily(File file) {
        return file.getName().toLowerCase().contains("makefile");
    }

    private TafMakefileProperty extractIncludeDirectory(File file) throws IOException {
        TafMakefileProperty tmp = new TafMakefileProperty();
        List<String> includes = new LinkedList<>();
        List<String> jceIncludes = new LinkedList<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            // APP       := MTT
            String[] fragments = line.split(":=");
            if (fragments.length == 2) {
                String key = fragments[0].trim();
                String value = fragments[1].trim();
                if (key.equals("APP")) {
                    tmp.setApp(value);
                }
                if (key.equals("TARGET")) {
                    tmp.setTarget(value);
                }
                if (key.equals("CFLAGS")) {
                    tmp.setCxxFlags(value);
                }
            }

            // INCLUDE   += -Iwsd -I/usr/local/mqq/wbl/include
            fragments = line.split("\\+=");
            if (fragments.length == 2) {
                String key = fragments[0].trim();
                String value = fragments[1].trim();
                if (key.equals("INCLUDE")) {
                    String[] includeFragments = value.split(" ");
                    for (String includeFragment : includeFragments) {
                        if (includeFragment.startsWith("-I")) {
                            includes.add(includeFragment.replace("-I", ""));
                        }
                    }
                }
            }
            if (line.startsWith("include") && line.endsWith(".mk")) {
                jceIncludes.add(line);
            }
        }
        bufferedReader.close();
        tmp.setIncludes(includes);
        tmp.setJceIncludes(jceIncludes);
        return tmp;
    }
}
