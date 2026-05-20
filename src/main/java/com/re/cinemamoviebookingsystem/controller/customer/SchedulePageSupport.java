package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.dto.response.ScheduleMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.ScheduleSlotDto;
import com.re.cinemamoviebookingsystem.util.CountryNames;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class SchedulePageSupport {

    static final List<String> AGE_FILTER_OPTIONS = List.of("P", "K", "T13", "T16", "T18");
    static final List<String> FORMAT_FILTER_OPTIONS = List.of("2D", "3D");
    static final List<String> ORIGIN_FILTER_OPTIONS = CountryNames.FILTER_CODES;

    private static final DateTimeFormatter SLOT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private SchedulePageSupport() {
    }

    static String normalizeSort(String sort) {
        if (sort != null && sort.equalsIgnoreCase("time")) {
            return "time";
        }
        return "name";
    }

    static String normalizeView(String view) {
        if (view != null && view.equalsIgnoreCase("list")) {
            return "list";
        }
        return "grid";
    }

    static List<ScheduleMovieCardDto> sortMovies(List<ScheduleMovieCardDto> cards, String sort) {
        if (cards == null || cards.isEmpty()) {
            return cards != null ? cards : List.of();
        }
        List<ScheduleMovieCardDto> copy = new ArrayList<>(cards);
        if ("time".equals(normalizeSort(sort))) {
            copy.sort(Comparator
                    .comparing(SchedulePageSupport::earliestSlot)
                    .thenComparing(ScheduleMovieCardDto::getTitle, String.CASE_INSENSITIVE_ORDER));
        } else {
            copy.sort(Comparator.comparing(ScheduleMovieCardDto::getTitle, String.CASE_INSENSITIVE_ORDER));
        }
        return copy;
    }

    private static LocalTime earliestSlot(ScheduleMovieCardDto card) {
        if (card.getSlots() == null || card.getSlots().isEmpty()) {
            return LocalTime.MAX;
        }
        return card.getSlots().stream()
                .map(SchedulePageSupport::parseSlotTime)
                .min(Comparator.naturalOrder())
                .orElse(LocalTime.MAX);
    }

    private static LocalTime parseSlotTime(ScheduleSlotDto slot) {
        if (slot == null || slot.getTimeLabel() == null) {
            return LocalTime.MAX;
        }
        try {
            return LocalTime.parse(slot.getTimeLabel().trim(), SLOT_TIME);
        } catch (DateTimeParseException ex) {
            return LocalTime.MAX;
        }
    }

    static List<String> normalizeAgeFilters(List<String> ages) {
        if (ages == null || ages.isEmpty()) {
            return List.of();
        }
        return ages.stream()
                .filter(a -> a != null && !a.isBlank())
                .map(a -> a.trim().toUpperCase(Locale.ROOT))
                .filter(AGE_FILTER_OPTIONS::contains)
                .distinct()
                .toList();
    }

    static List<String> normalizeFormatFilters(List<String> formats) {
        if (formats == null || formats.isEmpty()) {
            return List.of();
        }
        return formats.stream()
                .filter(f -> f != null && !f.isBlank())
                .map(f -> f.trim().toUpperCase(Locale.ROOT))
                .filter(FORMAT_FILTER_OPTIONS::contains)
                .distinct()
                .toList();
    }

    static List<String> normalizeOriginFilters(List<String> origins) {
        if (origins == null || origins.isEmpty()) {
            return List.of();
        }
        return origins.stream()
                .filter(o -> o != null && !o.isBlank())
                .map(o -> o.trim().toUpperCase(Locale.ROOT))
                .filter(CountryNames::isFilterCode)
                .distinct()
                .toList();
    }

    static List<ScheduleMovieCardDto> applyExtraFilters(List<ScheduleMovieCardDto> cards,
                                                       List<String> ages,
                                                       List<String> formats,
                                                       List<String> origins,
                                                       boolean availableOnly) {
        if (cards == null || cards.isEmpty()) {
            return cards != null ? cards : List.of();
        }
        List<ScheduleMovieCardDto> result = cards;
        if (!ages.isEmpty()) {
            result = result.stream()
                    .filter(c -> c.getAgeLabel() != null
                            && ages.contains(c.getAgeLabel().toUpperCase(Locale.ROOT)))
                    .toList();
        }
        if (!formats.isEmpty()) {
            result = result.stream()
                    .filter(c -> c.getFormat() != null
                            && formats.contains(c.getFormat().toUpperCase(Locale.ROOT)))
                    .toList();
        }
        if (!origins.isEmpty()) {
            result = result.stream()
                    .filter(c -> matchesOrigin(c, origins))
                    .toList();
        }
        if (availableOnly) {
            result = result.stream()
                    .filter(SchedulePageSupport::hasAvailableSlot)
                    .toList();
        }
        return result;
    }

    private static boolean matchesOrigin(ScheduleMovieCardDto card, List<String> origins) {
        if (card.getOriginCountryCode() != null
                && origins.contains(card.getOriginCountryCode().toUpperCase(Locale.ROOT))) {
            return true;
        }
        if (card.getOriginLabel() == null) {
            return false;
        }
        String label = card.getOriginLabel().trim();
        for (String code : origins) {
            String expected = CountryNames.label(code);
            if (expected != null && expected.equalsIgnoreCase(label)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAvailableSlot(ScheduleMovieCardDto card) {
        if (card.getSlots() == null || card.getSlots().isEmpty()) {
            return false;
        }
        return card.getSlots().stream().anyMatch(s -> s != null && !s.isSoldOut());
    }

    static boolean isFiltering(String q, String genre, List<String> ages, List<String> formats,
                               List<String> origins, boolean availableOnly) {
        return (q != null && !q.isBlank())
                || (genre != null && !genre.isBlank())
                || !ages.isEmpty()
                || !formats.isEmpty()
                || !origins.isEmpty()
                || availableOnly;
    }

}
