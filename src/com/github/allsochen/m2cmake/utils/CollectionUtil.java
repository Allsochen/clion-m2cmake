package com.github.allsochen.m2cmake.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionUtil {

    public static List<String> uniq(List<String> list) {
        Set<String> set = new HashSet<>(list);
        return new ArrayList<>(set);
    }

}
