package com.github.allsochen.m2cmake.configuration;

import com.github.allsochen.m2cmake.constants.Constants;
import com.github.allsochen.m2cmake.utils.OsInfo;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

public class JsonConfigBuilder {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private static final JsonConfigBuilder INSTANCE = new JsonConfigBuilder();

    private JsonConfigBuilder() {
    }

    public static JsonConfigBuilder getInstance() {
        return INSTANCE;
    }

    public String create() {
        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.setCmakeVersion("3.1");

        String username = System.getenv().get("USERNAME");
        String localDir = "D:/Codes/tafjce";
        if (!OsInfo.isWindows()) {
            localDir = "/Users/" + username + "/Codes/tafjce";
        }
        Map<String, String> mappings = new HashMap<>();
        mappings.put(Constants.HOME_TAFJCE + "/", localDir);
        jsonConfig.setDirMappings(mappings);

        List<String> includes = new ArrayList<>();
        if (OsInfo.isWindows()) {
            includes.add("D:/Codes/C++/taf/include");
            includes.add("D:/Codes/C++/taf/src");
        }
        jsonConfig.setIncludes(includes);
        jsonConfig.setAutomaticReloadCMake(true);

        List<String> remoteDirs = new ArrayList<>();
        if (OsInfo.isWindows()) {
            remoteDirs.add("Z:/tafjce");
        } else {
            remoteDirs.add("/Volumes/dev/tafjce");
            remoteDirs.add("/Volumes/dev/" + username + "/proejcts/MTT");
        }
        jsonConfig.setTafjceRemoteDirs(remoteDirs);

        jsonConfig.setTafjceLocalDir(localDir);

        jsonConfig.setNoForceSyncModules(defaultNoForceSyncModules());
        return gson.toJson(jsonConfig);
    }

    public static List<String> defaultNoForceSyncModules() {
        String[] noForceSyncModules = new String[]{
                "atta_api",
                "atta_log",
                "attaapi",
                "AttaLogReport",
                "AutoParameterOptimizerCppSdk",
                "bazel_tools",
                "boringssl",
                "cmod",
                "com_github_boostorg_preprocessor",
                "com_github_brpc_brpc",
                "com_github_cameron314_concurrentqueue",
                "com_github_gflags_gflags",
                "com_github_google_flatbuffers",
                "com_github_jbeder_yaml_cpp",
                "com_github_opentelemetry_proto",
                "com_github_robinhood_hash",
                "com_github_tencent_rapidjson",
                "com_github_toml11",
                "com_github_toml11",
                "com_gitlab_libeigen_eigen",
                "com_google_absl",
                "com_google_protobuf",
                "com_googlesource_code_re2",
                "concurrentqueue",
                "curl",
                "dcache_trpc",
                "didagle",
                "fbs_tool",
                "fmtlib",
                "forward_index_proto",
                "googlemock",
                "googletest",
                "ispine",
                "jce_tool",
                "jsoncpp",
                "kcfg",
                "LatestHistory",
                "local_config_cc",
                "local_curl",
                "LockFreeHashMap",
                "one_piece_proto",
                "opentelemetry_cpp",
                "opentracing_extended",
                "PcgMonitorApi",
                "picohttpparser",
                "platforms",
                "polaris_api",
                "polaris_api",
                "protobuf_archive",
                "rainbow_sdk",
                "segv_api",
                "snappy",
                "spdlog",
                "ssexpr",
                "taf",
                "taf_common",
                "TafCommon",
                "tconf_api",
                "tconv",
                "TegLogApi",
                "TegMonitorApi",
                "tensorflow",
                "third_party",
                "tjg_report_api",
                "tjgtracer",
                "tps_sdk_cpp",
                "trpc-pb",
                "trpc_cpp",
                "trpc_metis",
                "trs-interface",
                "trs_reranking",
                "zlib",
        };
        return Arrays.asList(noForceSyncModules);
    }

    public JsonConfig deserialize(String json) {
        return gson.fromJson(json, JsonConfig.class);
    }
}
