package com.sgtg.backend.domain.utils;

import java.util.List;

public abstract class CheckString {

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isBlank(List<String> str) {
        return str == null || str.isEmpty();
    }

}
