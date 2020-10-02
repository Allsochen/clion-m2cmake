package com.github.allsochen.m2cmake.makefile;

import com.github.allsochen.m2cmake.configuration.JsonConfigBuilder;

import java.util.ArrayList;
import java.util.List;

enum BazelFunctionalType {
    UNKNOWN,
    WORKSPACE,
    GIT_REPOSITORY,
    HTTP_ARCHIVE,
}

public class BazelWorkspace {

    private List<BazelFunctional> functionals;

    BazelWorkspace() {
        this.functionals = new ArrayList<>();
    }

    public String getTarget() {
        for (BazelFunctional bazelFunctional : functionals) {
            if (bazelFunctional.getType() == BazelFunctionalType.WORKSPACE) {
                return bazelFunctional.getName();
            }
        }
        return "";
    }

    public List<BazelFunctional> getDependencies() {
        List<BazelFunctional> dependencies = new ArrayList<>();
        for (BazelFunctional bazelFunctional : functionals) {
            if (bazelFunctional.getType() == BazelFunctionalType.GIT_REPOSITORY) {
                dependencies.add(bazelFunctional);
            }
        }
        return dependencies;
    }

    public List<String> getDependenceName() {
        List<String> dependencies = new ArrayList<>();
        for (BazelFunctional bazelFunctional : functionals) {
            if (bazelFunctional.getType() == BazelFunctionalType.GIT_REPOSITORY ||
                    bazelFunctional.getType() == BazelFunctionalType.HTTP_ARCHIVE) {
                dependencies.add(bazelFunctional.getName());
            }
        }
        return dependencies;
    }

    public boolean isValid() {
        return !functionals.isEmpty() || !getTarget().isEmpty();
    }

    public void merge(BazelWorkspace bazelWorkspace) {
        for (BazelFunctional bazelFunctional : bazelWorkspace.getFunctionals()) {
            add(bazelFunctional);
        }
    }

    public void add(List<BazelFunctional> bazelFunctionals) {
        for (BazelFunctional bazelFunctional : bazelFunctionals) {
            add(bazelFunctional);
        }
    }

    public boolean add(BazelFunctional bazelFunctional) {
        for (BazelFunctional functional : functionals) {
            if (functional.getType() == bazelFunctional.getType() &&
                    functional.getName().equals(bazelFunctional.getName())) {
                return false;
            }
        }
        functionals.add(bazelFunctional);
        return true;
    }

    public List<BazelFunctional> getFunctionals() {
        return functionals;
    }

    public static List<BazelFunctional> defaultFunctionals() {
        List<BazelFunctional> functionals = new ArrayList<>();
        List<String> modules = JsonConfigBuilder.defaultNoForceSyncModules();
        for (String module : modules) {
            BazelFunctional bazelFunctional = new BazelFunctional();
            bazelFunctional.setType(BazelFunctionalType.GIT_REPOSITORY);
            bazelFunctional.setName(module);
            functionals.add(bazelFunctional);
        }
        return functionals;
    }
}
