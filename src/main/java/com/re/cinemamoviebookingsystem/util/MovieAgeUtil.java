package com.re.cinemamoviebookingsystem.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MovieAgeUtil {

    private static final Pattern AGE_TAG = Pattern.compile("(?:^|[^0-9])T(\\d{1,2})(?:[^0-9]|$)|\\b(K|P)\\b", Pattern.CASE_INSENSITIVE);

    private MovieAgeUtil() {
    }

    public static String extractAgeLabel(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        Matcher m = AGE_TAG.matcher(title);
        if (!m.find()) {
            return null;
        }
        if (m.group(2) != null) {
            return m.group(2).toUpperCase();
        }
        return "T" + m.group(1);
    }

    public static String buildAgeNote(String title) {
        String label = extractAgeLabel(title);
        if (label == null) {
            return null;
        }
        return switch (label.toUpperCase()) {
            case "K" -> "Phim được phổ biến đến người xem dưới 13 tuổi";
            case "P" -> "Phim được phổ biến đến mọi đối tượng khán giả";
            case "T13" -> "Phim được phổ biến đến người xem từ đủ 13 tuổi trở lên";
            case "T16" -> "Phim được phổ biến đến người xem từ đủ 16 tuổi trở lên";
            case "T18" -> "Phim được phổ biến đến người xem từ đủ 18 tuổi trở lên";
            default -> {
                if (label.startsWith("T")) {
                    String age = label.substring(1);
                    yield "Phim được phổ biến đến người xem từ đủ " + age + " tuổi trở lên";
                }
                yield null;
            }
        };
    }
}
