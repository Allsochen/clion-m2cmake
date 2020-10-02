package com.github.allsochen.m2cmake.makefile;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class BazelFunctional {
    private BazelFunctionalType type;
    private String name;
    private LinkedHashMap<String, ArrayList<String>> options = new LinkedHashMap<>();

    public BazelFunctional() {
    }

    public BazelFunctional(BazelFunctionalType type, String name) {
        this.type = type;
        this.name = name;
    }

    public BazelFunctionalType getType() {
        return type;
    }

    public void setType(BazelFunctionalType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LinkedHashMap<String, ArrayList<String>> getOptions() {
        return options;
    }

    public void setOptions(LinkedHashMap<String, ArrayList<String>> options) {
        this.options = options;
    }
}
