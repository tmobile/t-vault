package com.tmobile.cso.vault.api.utils;

import java.util.HashMap;
import java.util.Map;

public class ThreadLocalContext {


    private static final ThreadLocal<Map<String, String>>  CURRENT = new ThreadLocal<Map<String, String>> (){
        @Override
        protected HashMap<String, String> initialValue() {
            return new HashMap<>();
        }
    };
    public static Map<String, String> getCurrentMap() {
        return CURRENT.get();
    }

    public static void setCurrentMap(Map<String, String> map) {
        CURRENT.set(map);
    }
}
