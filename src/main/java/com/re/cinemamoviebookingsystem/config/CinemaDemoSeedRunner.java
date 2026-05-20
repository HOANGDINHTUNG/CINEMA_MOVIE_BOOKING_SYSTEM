package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.repository.MovieRepository;
import com.re.cinemamoviebookingsystem.repository.RoomRepository;
import com.re.cinemamoviebookingsystem.service.CinemaCatalogService;
import com.re.cinemamoviebookingsystem.service.CinemaMovieService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Đồng bộ {@link DemoTmdbCatalog} vào DB mỗi lần khởi động: đăng phim còn thiếu (không bỏ qua vì đã đủ target cũ).
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class CinemaDemoSeedRunner implements ApplicationRunner {

    private final CinemaProperties cinemaProperties;
    private final RoomRepository roomRepository;
    private final MovieRepository movieRepository;
    private final CinemaCatalogService cinemaCatalogService;
    private final CinemaMovieService cinemaMovieService;

    @Override
    public void run(ApplicationArguments args) {
        if (!cinemaProperties.isDemoSeedOnStartup()) {
            log.info("Demo seed tắt (cinema.demo-seed-on-startup=false)");
            return;
        }
        if (roomRepository.count() == 0) {
            log.warn("Demo seed skipped: chưa có phòng chiếu trong DB");
            return;
        }

        int nowBefore = cinemaCatalogService.countNowShowingAtCinema();
        int soonBefore = cinemaCatalogService.countPublishedWithoutShowtimes();

        log.info(
                "Demo seed: đồng bộ catalog TMDB ({} id đang chiếu, {} id sắp chiếu). Hiện có: đang chiếu={}, sắp chiếu={}",
                DemoTmdbCatalog.NOW_SHOWING_TMDB_IDS.size(),
                DemoTmdbCatalog.COMING_SOON_TMDB_IDS.size(),
                nowBefore,
                soonBefore
        );

        AppLanguage lang = AppLanguage.VI_VN;
        int publishedNow = syncCatalog(DemoTmdbCatalog.NOW_SHOWING_TMDB_IDS, lang, true);
        int publishedSoon = syncCatalog(DemoTmdbCatalog.COMING_SOON_TMDB_IDS, lang, false);

        int nowAfter = cinemaCatalogService.countNowShowingAtCinema();
        int soonAfter = cinemaCatalogService.countPublishedWithoutShowtimes();

        log.info(
                "Demo seed xong: +{} phim đang chiếu (có lịch), +{} phim sắp chiếu. Tổng: đang chiếu={}/{}, sắp chiếu={}/{}",
                publishedNow,
                publishedSoon,
                nowAfter,
                cinemaProperties.getDemoSeedNowShowingTarget(),
                soonAfter,
                cinemaProperties.getDemoSeedComingSoonTarget()
        );

        if (publishedNow == 0 && publishedSoon == 0
                && nowAfter < cinemaProperties.getDemoSeedNowShowingTarget()) {
            log.warn(
                    "Không đăng thêm phim mới — kiểm tra TMDB_API_KEY trong application-local.properties và log 'publish failed' phía trên");
        }
    }

    private int syncCatalog(List<Long> tmdbIds, AppLanguage lang, boolean withSchedule) {
        int published = 0;
        int skipped = 0;
        int failed = 0;
        for (Long tmdbId : tmdbIds) {
            if (movieRepository.existsByTmdbId(tmdbId)) {
                skipped++;
                continue;
            }
            try {
                cinemaMovieService.publishToCinema(tmdbId, lang, null, withSchedule);
                published++;
                pauseBriefly();
            } catch (Exception ex) {
                failed++;
                log.warn("Demo publish failed tmdbId={} schedule={}: {}", tmdbId, withSchedule, ex.getMessage());
            }
        }
        log.info("Catalog sync (schedule={}): +{} mới, {} đã có, {} lỗi / {} id",
                withSchedule, published, skipped, failed, tmdbIds.size());
        return published;
    }

    private static void pauseBriefly() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
