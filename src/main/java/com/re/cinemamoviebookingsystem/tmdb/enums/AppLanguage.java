package com.re.cinemamoviebookingsystem.tmdb.enums;

import java.util.Locale;

public enum AppLanguage {
    VI_VN("vi-VN"),
    EN_US("en-US");

    private final String tmdbCode;

    AppLanguage(String tmdbCode) {
        this.tmdbCode = tmdbCode;
    }

    public String getTmdbCode() {
        return tmdbCode;
    }

    public static AppLanguage fromParam(String lang) {
        if (lang == null || lang.isBlank()) {
            return VI_VN;
        }
        String normalized = lang.trim();
        for (AppLanguage value : values()) {
            if (value.tmdbCode.equalsIgnoreCase(normalized)) {
                return value;
            }
        }
        if ("vi".equalsIgnoreCase(normalized) || "vi_VN".equalsIgnoreCase(normalized)) {
            return VI_VN;
        }
        if ("en".equalsIgnoreCase(normalized) || "en_US".equalsIgnoreCase(normalized)) {
            return EN_US;
        }
        return VI_VN;
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(tmdbCode);
    }
}
