package com.re.cinemamoviebookingsystem.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CountryNames {

    /** Mã quốc gia dùng cho bộ lọc xuất xứ trên lịch chiếu. */
    public static final List<String> FILTER_CODES = List.of(
            "VN", "US", "KR", "JP", "CN", "TH", "GB", "FR", "DE", "IN", "AU", "CA", "HK", "TW");

    private static final Map<String, String> VI_LABELS = Map.ofEntries(
            Map.entry("US", "Mỹ"),
            Map.entry("VN", "Việt Nam"),
            Map.entry("TH", "Thái Lan"),
            Map.entry("KR", "Hàn Quốc"),
            Map.entry("JP", "Nhật Bản"),
            Map.entry("CN", "Trung Quốc"),
            Map.entry("GB", "Anh"),
            Map.entry("FR", "Pháp"),
            Map.entry("DE", "Đức"),
            Map.entry("IN", "Ấn Độ"),
            Map.entry("AU", "Úc"),
            Map.entry("CA", "Canada"),
            Map.entry("HK", "Hồng Kông"),
            Map.entry("TW", "Đài Loan")
    );

    private static final Map<String, String> EN_LABELS = Map.ofEntries(
            Map.entry("US", "United States"),
            Map.entry("VN", "Vietnam"),
            Map.entry("TH", "Thailand"),
            Map.entry("KR", "South Korea"),
            Map.entry("JP", "Japan"),
            Map.entry("CN", "China"),
            Map.entry("GB", "United Kingdom"),
            Map.entry("FR", "France"),
            Map.entry("DE", "Germany"),
            Map.entry("IN", "India"),
            Map.entry("AU", "Australia"),
            Map.entry("CA", "Canada"),
            Map.entry("HK", "Hong Kong"),
            Map.entry("TW", "Taiwan")
    );

    private CountryNames() {
    }

    public static String label(String isoCode) {
        return label(isoCode, Locale.forLanguageTag("vi"));
    }

    public static String label(String isoCode, Locale locale) {
        if (isoCode == null || isoCode.isBlank()) {
            return null;
        }
        String key = isoCode.trim().toUpperCase(Locale.ROOT);
        Map<String, String> labels = locale != null && locale.getLanguage().startsWith("en")
                ? EN_LABELS
                : VI_LABELS;
        return labels.getOrDefault(key, key);
    }

    public static boolean isFilterCode(String isoCode) {
        if (isoCode == null || isoCode.isBlank()) {
            return false;
        }
        return FILTER_CODES.contains(isoCode.trim().toUpperCase(Locale.ROOT));
    }

    /** JSON object code → label for client-side filter chips. */
    public static String labelsJsonForLocale(Locale locale) {
        Map<String, String> labels = locale != null && locale.getLanguage().startsWith("en")
                ? EN_LABELS
                : VI_LABELS;
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < FILTER_CODES.size(); i++) {
            String code = FILTER_CODES.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(code).append("\":\"")
                    .append(escapeJson(labels.getOrDefault(code, code))).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
