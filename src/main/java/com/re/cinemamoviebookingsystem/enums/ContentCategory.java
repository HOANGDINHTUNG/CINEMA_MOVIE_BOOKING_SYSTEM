package com.re.cinemamoviebookingsystem.enums;

import lombok.Getter;

@Getter
public enum ContentCategory {
    PROMOTION("/assets/img/events/"),
    NEWS("/assets/img/events/"),
    FESTIVAL("/assets/img/eventFestival/");

    private final String imageBase;

    ContentCategory(String imageBase) {
        this.imageBase = imageBase;
    }

    public String seedClasspathFile() {
        return switch (this) {
            case PROMOTION -> "database/promotion.json";
            case NEWS -> "database/event.json";
            case FESTIVAL -> "database/festival.json";
        };
    }
}
