package com.re.cinemamoviebookingsystem.config;

import java.util.List;
import java.util.Set;

/**
 * Danh sách TMDB id cho demo seed — cập nhật bằng {@code scripts/fetch_tmdb_demo_catalog.py}.
 * <ul>
 *   <li>{@link #WAITING_SCHEDULE_TMDB_IDS} — TMDB now_playing, đăng rạp <strong>không</strong> tạo suất (admin: Đang đợi lịch chiếu)</li>
 *   <li>{@link #DEMO_SCHEDULED_TMDB_IDS} — id riêng (không trùng waiting), có lịch mẫu nếu {@code demo-seed-scheduled-target &gt; 0}</li>
 * </ul>
 */
public final class DemoTmdbCatalog {

    private DemoTmdbCatalog() {
    }

    /** TMDB now_playing — «Đang đợi lịch chiếu» (không suất). */
    public static final List<Long> WAITING_SCHEDULE_TMDB_IDS = List.of(
            1439930L, 687163L, 1304313L, 1339713L, 931285L, 936075L, 1226863L, 1228710L, 1314481L, 1007757L,
            1330021L, 1102883L, 1140521L, 855435L, 1455079L, 83533L, 1327819L, 1582770L, 1242332L, 1122573L,
            1083381L, 1684226L, 1273221L, 1325734L, 1301421L, 1266127L, 1368337L, 1317288L, 1159831L, 1198994L,
            1297842L, 1084244L, 1292695L, 1368314L, 1430077L, 680493L, 696393L, 1380316L, 1405769L, 1630423L,
            1316092L, 454639L, 969681L, 1368166L, 1400357L, 1292415L, 1275779L, 1290821L, 1214931L, 1084242L,
            1662317L, 1108427L, 1628367L, 1367220L, 1684240L, 840464L, 1656061L, 1280738L, 1669050L, 1620034L,
            467905L, 1542261L, 1116201L, 1215106L, 910850L, 1319522L, 1146058L, 1400499L, 1508591L, 1218067L,
            608067L, 1264821L, 1139636L, 1591972L, 1667685L, 1641479L, 1512207L, 1607088L, 1665785L, 1650910L,
            1671608L, 1697695L, 350L, 1318447L, 460465L, 249397L, 1419406L, 1311031L, 1613798L, 1242898L,
            157336L, 78192L, 318256L, 1579L, 24428L, 1171145L, 278L, 458293L, 1159559L, 1265609L
    );

    /**
     * Phim demo «Đã có lịch» — id không nằm trong {@link #WAITING_SCHEDULE_TMDB_IDS} (trending, không dùng upcoming cho waiting).
     */
    public static final List<Long> DEMO_SCHEDULED_TMDB_IDS = List.of(
            372058L, 1145899L, 1497348L, 1532494L, 1671716L, 1575667L, 1689165L, 1698414L, 1698407L, 1110034L,
            1340206L, 1057265L, 1318413L, 1295400L, 13754L, 1284016L, 1119090L, 1694978L, 1440050L, 1541560L,
            1470329L, 1083884L, 1245859L, 1665527L, 1329471L
    );

    private static final Set<Long> WAITING_ID_SET = Set.copyOf(WAITING_SCHEDULE_TMDB_IDS);

    static {
        for (Long id : DEMO_SCHEDULED_TMDB_IDS) {
            if (WAITING_ID_SET.contains(id)) {
                throw new IllegalStateException("DEMO_SCHEDULED_TMDB_IDS trùng WAITING_SCHEDULE_TMDB_IDS: " + id);
            }
        }
    }

    /** @deprecated Dùng {@link #WAITING_SCHEDULE_TMDB_IDS}. */
    @Deprecated
    public static final List<Long> NOW_SHOWING_TMDB_IDS = WAITING_SCHEDULE_TMDB_IDS;

    /** @deprecated Không dùng upcoming cho seed «đang đợi». */
    @Deprecated
    public static final List<Long> COMING_SOON_TMDB_IDS = List.of();

    public static boolean isWaitingPoolTmdbId(long tmdbId) {
        return WAITING_ID_SET.contains(tmdbId);
    }

    public static Set<Long> waitingIdSet() {
        return WAITING_ID_SET;
    }
}
