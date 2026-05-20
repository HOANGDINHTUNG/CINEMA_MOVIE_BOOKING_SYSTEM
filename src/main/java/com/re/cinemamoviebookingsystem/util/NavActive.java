package com.re.cinemamoviebookingsystem.util;

/**
 * Xác định mục menu header đang active theo đường dẫn customer.
 */
public final class NavActive {

    private NavActive() {
    }

    public static String resolve(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        if (path.equals("/customer") || path.equals("/customer/") || path.startsWith("/customer/home")) {
            return "home";
        }
        if (path.startsWith("/customer/calendar")) {
            return "calendar";
        }
        if (path.startsWith("/customer/news")) {
            return "news";
        }
        if (path.startsWith("/customer/promotions")) {
            return "promotions";
        }
        if (path.startsWith("/customer/ticket-price")) {
            return "ticket-price";
        }
        if (path.startsWith("/customer/festival")) {
            return "festival";
        }
        return "";
    }

    public static String accountSection(String path) {
        if (path == null) {
            return "";
        }
        if (path.startsWith("/customer/account/security")) {
            return "security";
        }
        if (path.startsWith("/customer/profile")) {
            return "profile";
        }
        if (path.startsWith("/customer/bookings")) {
            return "bookings";
        }
        if (path.startsWith("/customer/account")) {
            return "overview";
        }
        return "";
    }

    public static String initials(String fullName, String username) {
        String source = fullName != null && !fullName.isBlank() ? fullName.trim() : username;
        if (source == null || source.isBlank()) {
            return "?";
        }
        String[] parts = source.split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return ("" + source.charAt(0)).toUpperCase();
    }
}
