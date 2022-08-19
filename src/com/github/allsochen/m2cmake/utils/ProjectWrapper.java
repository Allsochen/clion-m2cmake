package com.github.allsochen.m2cmake.utils;

import com.intellij.openapi.project.Project;

public class ProjectWrapper {

    private String app;
    private String target;
    private Project project;

    public ProjectWrapper(String app, String target, Project project) {
        this.app = app;
        this.target = target;
        this.project = project;
    }

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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
