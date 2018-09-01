package com.github.allsochen.m2cmake.configuration;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonConfigBuilder {

    private Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private static JsonConfigBuilder instance = new JsonConfigBuilder();

    private JsonConfigBuilder() {
    }

    public static JsonConfigBuilder getInstance() {
        return instance;
    }

    public String create() {
        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.setCmakeVersion("3.1");
        Map<String, String> mappings = new HashMap<>();

        mappings.put("/home/tafjce/", "D:/Codes/tafjce/");
        jsonConfig.setDirMappings(mappings);

        List<String> includes = new ArrayList<>();
        includes.add("D:/Codes/C++/taf/include");
        includes.add("D:/Codes/C++/taf/src");
        jsonConfig.setIncludes(includes);
        jsonConfig.setAutomaticReloadCMake(true);
        return gson.toJson(jsonConfig);
    }

    public JsonConfig deserialize(String json) {
        return gson.fromJson(json, JsonConfig.class);
    }
}
