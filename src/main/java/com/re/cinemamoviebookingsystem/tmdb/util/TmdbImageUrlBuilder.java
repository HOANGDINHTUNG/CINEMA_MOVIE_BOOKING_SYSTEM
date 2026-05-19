package com.re.cinemamoviebookingsystem.tmdb.util;

import com.re.cinemamoviebookingsystem.tmdb.config.TmdbProperties;
import org.springframework.stereotype.Component;

@Component
public class TmdbImageUrlBuilder {

    private final String imageBase;

    public TmdbImageUrlBuilder(TmdbProperties properties) {
        String base = properties.imageBase();
        if (base == null || base.isBlank()) {
            this.imageBase = "https://image.tmdb.org/t/p/";
        } else {
            this.imageBase = base.endsWith("/") ? base : base + "/";
        }
    }

    public String build(String size, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return imageBase + size + normalizedPath;
    }

    public String posterW500(String path) {
        return build("w500", path);
    }

    public String backdropOriginal(String path) {
        return build("original", path);
    }
}
