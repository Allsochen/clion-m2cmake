package com.github.allsochen.m2cmake.makefile;

import java.util.ArrayList;
import java.util.List;

enum BazelFunctionalType {
    UNKNOWN,
    WORKSPACE,
    GIT_REPOSITORY,
}

public class BazelWorkspace {

    private List<BazelFunctional> functionals;

    BazelWorkspace() {
        this.functionals = new ArrayList<>();

        // Add the google protobuf as default.
        BazelFunctional bazelFunctional = new BazelFunctional();
        bazelFunctional.setType(BazelFunctionalType.GIT_REPOSITORY);
        bazelFunctional.setName("com_google_protobuf");
        this.functionals.add(bazelFunctional);
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
            if (bazelFunctional.getType() == BazelFunctionalType.GIT_REPOSITORY) {
                dependencies.add(bazelFunctional.getName());
            }
        }
        return dependencies;
    }

    public boolean isValid() {
        return !functionals.isEmpty();
    }

    public void merge(BazelWorkspace bazelWorkspace) {
        for (BazelFunctional bazelFunctional : bazelWorkspace.getFunctionals()) {
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
}
