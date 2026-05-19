package com.re.cinemamoviebookingsystem.tmdb;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map movie_id noi bo (seed) -> tmdb_id.
 */
public final class TmdbSeedMappings {

    private static final Map<Long, Long> BY_MOVIE_ID;

    static {
        Map<Long, Long> map = new LinkedHashMap<>();
        map.put(1L, 299534L);   // Avengers: Endgame
        map.put(2L, 1146460L);  // Mai (2024)
        map.put(3L, 1163308L);  // Doraemon: Nobita's Earth Symphony
        map.put(4L, 1305653L);  // Lat Mat 8 (approx)
        map.put(5L, 1022789L);  // Inside Out 2
        map.put(6L, 533535L);   // Deadpool & Wolverine
        map.put(7L, 1008434L);  // Cong tu Bac Lieu (approx)
        map.put(8L, 823464L);   // Godzilla x Kong
        map.put(9L, 1011985L);  // Kung Fu Panda 4
        map.put(10L, 872585L);  // Oppenheimer
        map.put(11L, 346698L);  // Barbie
        map.put(12L, 1001274L); // Nha ba Nu (approx)
        map.put(13L, 838050L);  // Exhuma
        map.put(14L, 693134L);  // Dune: Part Two
        map.put(15L, 569094L);  // Spider-Verse franchise
        map.put(16L, 385687L);  // Fast X
        map.put(17L, 414906L);  // The Batman (part 2 chua co)
        map.put(18L, 1241982L); // Moana 2
        map.put(20L, 698687L);  // Transformers One
        BY_MOVIE_ID = Collections.unmodifiableMap(map);
    }

    private TmdbSeedMappings() {
    }

    public static Map<Long, Long> all() {
        return BY_MOVIE_ID;
    }
}
