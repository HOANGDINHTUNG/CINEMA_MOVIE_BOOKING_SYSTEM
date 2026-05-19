package com.re.cinemamoviebookingsystem.config;

import java.util.List;

/**
 * Danh sách TMDB id dùng cho demo seed (phim điện ảnh phổ biến).
 */
public final class DemoTmdbCatalog {

    private DemoTmdbCatalog() {
    }

    /** Đăng kèm lịch chiếu → mục «Phim đang chiếu». */
    public static final List<Long> NOW_SHOWING_TMDB_IDS = List.of(
            299534L, 299536L, 533535L, 1022789L, 157336L, 27205L, 438631L, 693134L,
            823464L, 1011985L, 872585L, 346698L, 1001274L, 838050L, 569094L, 385687L,
            414906L, 1241982L, 698687L, 1146460L, 1163308L, 1305653L, 1008434L, 284054L,
            181808L, 120L, 155L, 122L, 13L, 1891L, 680L, 129L, 14160L, 177572L,
            109445L, 519182L, 575264L, 667538L, 10195L, 24428L, 271110L, 49026L
    );

    /** Đăng không tạo suất → mục «Phim sắp chiếu» (đã đăng rạp, chưa có lịch). */
    public static final List<Long> COMING_SOON_TMDB_IDS = List.of(
            1087192L, 1311031L, 762509L, 617126L, 1116465L, 911916L, 608812L, 474350L,
            1034541L, 845781L, 1084736L, 1011983L, 106646L, 976573L, 956920L, 940721L,
            558449L, 359410L, 762968L, 11527L
    );
}
