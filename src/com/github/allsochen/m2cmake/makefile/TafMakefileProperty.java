package com.github.allsochen.m2cmake.makefile;

import java.util.ArrayList;
import java.util.List;

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

}
