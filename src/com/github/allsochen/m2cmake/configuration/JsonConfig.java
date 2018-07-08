package com.github.allsochen.m2cmake.configuration;

import java.util.List;
import java.util.Map;

public class JsonConfig {

    private String cmakeVersion;
    private List<String> includes;
    private Map<String, String> dirMappings;

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
}
