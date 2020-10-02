package com.github.allsochen.m2cmake.configuration;

import com.github.allsochen.m2cmake.utils.Constants;
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

        mappings.put(Constants.HOME_TAFJCE + "/", "D:/Codes/tafjce/");
        jsonConfig.setDirMappings(mappings);

        List<String> includes = new ArrayList<>();
        includes.add("D:/Codes/C++/taf/include");
        includes.add("D:/Codes/C++/taf/src");
        jsonConfig.setIncludes(includes);
        jsonConfig.setAutomaticReloadCMake(true);

        List<String> tafjceSourceDirs = new ArrayList<>();
        tafjceSourceDirs.add("Z:/tafjce");
        tafjceSourceDirs.add("Y:/tafjce");
        jsonConfig.setTafjceRemoteDirs(tafjceSourceDirs);

        jsonConfig.setTafjceLocalDir("D:/Codes/tafjce");

        jsonConfig.setNoForceSyncModules(defaultNoForceSyncModules());
        return gson.toJson(jsonConfig);
    }

    public static List<String> defaultNoForceSyncModules() {
        List<String> noForceSyncModules = new ArrayList<>();
        noForceSyncModules.add("com_github_gflags_gflags");
        noForceSyncModules.add("com_github_tencent_rapidjson");
        noForceSyncModules.add("com_github_jbeder_yaml_cpp");
        noForceSyncModules.add("com_google_protobuf");
        noForceSyncModules.add("bazel_tools");
        noForceSyncModules.add("zlib");
        noForceSyncModules.add("taf");
        noForceSyncModules.add("trpc_cpp");
        noForceSyncModules.add("dcache_trpc");
        noForceSyncModules.add("opentracing_extended");
        noForceSyncModules.add("googlemock");
        noForceSyncModules.add("googletest");
        noForceSyncModules.add("local_curl");
        noForceSyncModules.add("protobuf_archive");
        noForceSyncModules.add("rainbow_sdk");
        noForceSyncModules.add("tconf_api");
        noForceSyncModules.add("TegMonitorApi");
        noForceSyncModules.add("tjg_report_api");
        noForceSyncModules.add("tjgtracer");
        return noForceSyncModules;
    }

    public JsonConfig deserialize(String json) {
        return gson.fromJson(json, JsonConfig.class);
    }
}
