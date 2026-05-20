package com.re.cinemamoviebookingsystem.util;

/**
 * Maps combo name/description to Font Awesome icon classes for admin and checkout UI.
 */
public final class ComboIconSupport {

    private ComboIconSupport() {
    }

    public static String iconClass(String name, String description) {
        String text = ((name != null ? name : "") + " " + (description != null ? description : "")).toLowerCase();
        if (text.contains("kem") || text.contains("ice cream") || text.contains("ice-cream")) {
            return "fa-ice-cream";
        }
        if (text.contains("snack") || text.contains("gà") || text.contains("hot dog")) {
            return "fa-hotdog";
        }
        if (text.contains("energy") || text.contains("premium")) {
            return "fa-bolt";
        }
        if (text.contains("zero") || text.contains("không đường") || text.contains("diet")) {
            return "fa-leaf";
        }
        if (text.contains("student") || text.contains("sv")) {
            return "fa-graduation-cap";
        }
        if (text.contains("couple") || text.contains("đôi")) {
            return "fa-heart";
        }
        if (text.contains("family") || text.contains("gia đình") || text.contains("super")) {
            return "fa-users";
        }
        if (text.contains("solo")) {
            return "fa-user";
        }
        if (text.contains("bắp") || text.contains("popcorn")) {
            return "fa-popcorn";
        }
        if (text.contains("nước") || text.contains("coca") || text.contains("pepsi") || text.contains("drink")) {
            return "fa-glass-water";
        }
        return "fa-box-open";
    }
}
