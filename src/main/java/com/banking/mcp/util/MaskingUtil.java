package com.banking.mcp.util;

import java.util.*;
import java.util.regex.Pattern;

public class MaskingUtil {

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("\\b\\d{8,18}\\b");

    private MaskingUtil() {}

    // 🔷 Entry point
    public static Map<String, Object> mask(Map<String, Object> input) {
        if (input == null) return Collections.emptyMap();

        Map<String, Object> masked = new HashMap<>();

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            masked.put(entry.getKey(), maskValue(entry.getKey(), entry.getValue()));
        }

        return masked;
    }

    // 🔷 Recursive masking
    private static Object maskValue(String key, Object value) {

        if (value == null) return null;

        if (value instanceof String str) {
            return maskString(key, str);
        }

        if (value instanceof Number) {
            return maskNumber(key, (Number) value);
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                nested.put(
                        String.valueOf(e.getKey()),
                        maskValue(String.valueOf(e.getKey()), e.getValue())
                );
            }
            return nested;
        }

        if (value instanceof List<?> list) {
            List<Object> maskedList = new ArrayList<>();
            for (Object item : list) {
                maskedList.add(maskValue(key, item));
            }
            return maskedList;
        }

        return value; // safe fallback
    }

    // 🔷 String masking rules
    private static String maskString(String key, String value) {

        String lowerKey = key.toLowerCase();

        // Account IDs
        if (lowerKey.contains("account")) {
            return maskAccount(value);
        }

        // Names
        if (lowerKey.contains("name")) {
            return maskName(value);
        }

        // BIC / SWIFT
        if (lowerKey.contains("bic") || lowerKey.contains("swift")) {
            return maskBic(value);
        }

        // Payment IDs
        if (lowerKey.contains("payment")) {
            return maskGenericId(value);
        }

        // Generic numeric detection (fallback safety)
        if (ACCOUNT_PATTERN.matcher(value).find()) {
            return maskAccount(value);
        }

        return value;
    }

    // 🔷 Number masking
    private static Object maskNumber(String key, Number value) {

        if (key.toLowerCase().contains("amount")) {
            // Optional: bucket instead of exact value
            double val = value.doubleValue();

            if (val > 100000) return ">100K";
            if (val > 10000) return "10K-100K";
            if (val > 1000) return "1K-10K";

            return "<1K";
        }

        return value;
    }

    // 🔷 Helpers

    public static String maskAccount(String input) {
        if (input.length() <= 4) return "XXXX";

        String last4 = input.substring(input.length() - 4);
        return "XXXXXX" + last4;
    }

    private static String maskName(String name) {
        if (name.length() <= 2) return "*";

        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }

    private static String maskBic(String bic) {
        if (bic.length() <= 4) return "XXXX";

        return "XXXX" + bic.substring(bic.length() - 4);
    }

    private static String maskGenericId(String id) {
        if (id.length() <= 4) return "****";

        return "****" + id.substring(id.length() - 4);
    }
}