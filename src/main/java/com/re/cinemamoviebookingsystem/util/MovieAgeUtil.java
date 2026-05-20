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

    public static String resolveAgeLabel(String title, String ageLabel) {
        if (ageLabel != null && !ageLabel.isBlank()) {
            return ageLabel.trim().toUpperCase();
        }
        return extractAgeLabel(title);
    }

    public static String buildAgeNote(String title) {
        return buildAgeNote(title, null);
    }

    /** Luôn trả về mô tả độ tuổi (ưu tiên age_label DB, sau đó nhãn trong tên phim, mặc định P). */
    public static String buildAgeNote(String title, String ageLabel) {
        String label = resolveAgeLabel(title, ageLabel);
        if (label == null) {
            label = "P";
        }
        return formatAgeNote(label);
    }

    private static String formatAgeNote(String label) {
        String u = label.toUpperCase();
        String base = switch (u) {
            case "K" -> "Phim được phổ biến đến người xem dưới 13 tuổi";
            case "P" -> "Phim được phổ biến đến mọi đối tượng khán giả";
            case "T13" -> "Phim được phổ biến đến người xem từ đủ 13 tuổi trở lên";
            case "T16" -> "Phim được phổ biến đến người xem từ đủ 16 tuổi trở lên";
            case "T18" -> "Phim được phổ biến đến người xem từ đủ 18 tuổi trở lên";
            default -> {
                if (u.startsWith("T") && u.length() > 1) {
                    String age = u.substring(1);
                    yield "Phim được phổ biến đến người xem từ đủ " + age + " tuổi trở lên";
                }
                yield "Phim được phổ biến đến mọi đối tượng khán giả";
            }
        };
        if (u.startsWith("T") && u.length() > 1 && Character.isDigit(u.charAt(1))) {
            return base + " (" + u.substring(1) + "+)";
        }
        return base;
    }

    /** Ghép nhãn tuổi vào tiêu đề hiển thị, ví dụ: MỘT THỜI TA ĐÃ YÊU-T16 */
    public static String appendAgeSuffixToTitle(String title, String ageLabel) {
        if (title == null || title.isBlank()) {
            return title;
        }
        String label = resolveAgeLabel(title, ageLabel);
        if (label == null) {
            return title;
        }
        if (extractAgeLabel(title) != null) {
            return title;
        }
        String suffix = "-" + label;
        if (title.toUpperCase().contains(suffix.toUpperCase())) {
            return title;
        }
        return title.strip() + suffix;
    }
}
