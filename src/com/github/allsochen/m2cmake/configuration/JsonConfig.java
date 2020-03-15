package com.github.allsochen.m2cmake.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonConfig {

    // cmake_minimum_required(VERSION 3.1)
    private String cmakeVersion = "3.1";
    private List<String> includes = new ArrayList<>();
    private Map<String, String> dirMappings = new HashMap<>();
    private boolean automaticReloadCMake = true;
    private List<String> tafjceRemoteDirs = new ArrayList<>();
    private String tafjceLocalDir = "";
    private List<String> noForceSyncModules;

    public JsonConfig() {
        noForceSyncModules = JsonConfigBuilder.defaultNoForceSyncModules();
    }

    public String getCmakeVersion() {
        return cmakeVersion;
    }

    public void setCmakeVersion(String cmakeVersion) {
        this.cmakeVersion = cmakeVersion;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public Map<String, String> getDirMappings() {
        return dirMappings;
    }

    public void setDirMappings(Map<String, String> dirMappings) {
        this.dirMappings = dirMappings;
    }

    public boolean isAutomaticReloadCMake() {
        return automaticReloadCMake;
    }

    public void setAutomaticReloadCMake(boolean automaticReloadCMake) {
        this.automaticReloadCMake = automaticReloadCMake;
    }

    public List<String> getTafjceRemoteDirs() {
        return tafjceRemoteDirs;
    }

    public void setTafjceRemoteDirs(List<String> tafjceRemoteDirs) {
        this.tafjceRemoteDirs = tafjceRemoteDirs;
    }

    public String getTafjceLocalDir() {
        return tafjceLocalDir;
    }

    public void setTafjceLocalDir(String tafjceLocalDir) {
        this.tafjceLocalDir = tafjceLocalDir;
    }

    public List<String> getNoForceSyncModules() {
        return noForceSyncModules;
    }

    public void setNoForceSyncModules(List<String> noForceSyncModules) {
        this.noForceSyncModules = noForceSyncModules;
    }
}
