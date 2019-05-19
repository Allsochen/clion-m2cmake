package com.github.allsochen.m2cmake;

import com.github.allsochen.m2cmake.configuration.Configuration;
import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.configuration.JsonConfigBuilder;
import com.github.allsochen.m2cmake.configuration.Properties;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.util.ArrayList;
import java.util.List;

public class BaseAction {

    public static String chooseApp(String app) {
        if (app == null || app.isEmpty()) {
            return "MTT";
        }
        return app;
    }

    public static String chooseTarget(String projectName, List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            if (projectName == null || projectName.isEmpty()) {
                return "UnknownServer";
            }
            return projectName;
        }
        List<String> servers = new ArrayList<>();
        for (String target : targets) {
            if (target.equals(projectName)) {
                return target;
            }
            if (target.contains("Server")) {
                servers.add(target);
            }
            if (target.contains(".a")) {
                return projectName;
            }
        }
        if (!servers.isEmpty()) {
            return servers.get(0);
        }
        return projectName;
    }

    public static JsonConfig getJsonConfig(Project project) {
        String json = Properties.get(Configuration.JSON_STR);
        if (json == null || json.isEmpty()) {
            json = JsonConfigBuilder.getInstance().create();
        }
        JsonConfig jsonConfig = null;
        try {
            jsonConfig = JsonConfigBuilder.getInstance().deserialize(json);
        } catch (Exception e) {
            Messages.showInfoMessage(project,
                    "please check json configuration, Settings->TAF m2cmake configuration",
                    "m2cmake configuration error");
        }
        return jsonConfig;
    }

}
