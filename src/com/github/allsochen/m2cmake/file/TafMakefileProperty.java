package com.github.allsochen.m2cmake.file;

import java.util.LinkedList;
import java.util.List;

public class TafMakefileProperty {
    private String app;
    private String target;
    private List<String> includes = new LinkedList<>();
    private List<String> jceIncludes = new LinkedList<>();

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
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

    public void addIncludes(List<String> includes) {
        this.includes.addAll(includes);
    }

    public void addJceIncludes(List<String> jceIincludes) {
        this.jceIncludes.addAll(jceIincludes);
    }
}
