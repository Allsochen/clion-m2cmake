package com.github.allsochen.m2cmake.configuration;

import java.util.List;
import java.util.Map;

public class JsonConfig {

    // cmake_minimum_required(VERSION 3.1)
    private String cmakeVersion = "3.1";
    private List<String> includes;
    private Map<String, String> dirMappings;
    private boolean automaticReloadCMake = true;

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
}
