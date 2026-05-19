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

/**
 * Bổ sung dữ liệu demo: ~40 phim đang chiếu (có suất) + ~18 phim sắp chiếu (chưa có suất).
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
            return;
        }
        if (roomRepository.count() == 0) {
            log.warn("Demo seed skipped: no rooms in database");
            return;
        }

        int targetNow = cinemaProperties.getDemoSeedNowShowingTarget();
        int targetSoon = cinemaProperties.getDemoSeedComingSoonTarget();
        int nowHave = cinemaCatalogService.countNowShowingAtCinema();
        int soonHave = cinemaCatalogService.countComingSoonAtCinema();

        if (nowHave >= targetNow && soonHave >= targetSoon) {
            log.debug("Demo seed skipped: now={}/{} soon={}/{}", nowHave, targetNow, soonHave, targetSoon);
            return;
        }

        log.info("Demo seed: bổ sung phim (hiện có đang chiếu={}, sắp chiếu={})", nowHave, soonHave);
        AppLanguage lang = AppLanguage.VI_VN;

        int publishedNow = 0;
        for (Long tmdbId : DemoTmdbCatalog.NOW_SHOWING_TMDB_IDS) {
            if (nowHave + publishedNow >= targetNow) {
                break;
            }
            if (movieRepository.existsByTmdbId(tmdbId)) {
                continue;
            }
            try {
                cinemaMovieService.publishToCinema(tmdbId, lang, null, true);
                publishedNow++;
                pauseBriefly();
            } catch (Exception ex) {
                log.warn("Demo now-showing publish failed tmdbId={}: {}", tmdbId, ex.getMessage());
            }
        }

        int publishedSoon = 0;
        for (Long tmdbId : DemoTmdbCatalog.COMING_SOON_TMDB_IDS) {
            if (soonHave + publishedSoon >= targetSoon) {
                break;
            }
            if (movieRepository.existsByTmdbId(tmdbId)) {
                continue;
            }
            try {
                cinemaMovieService.publishToCinema(tmdbId, lang, null, false);
                publishedSoon++;
                pauseBriefly();
            } catch (Exception ex) {
                log.warn("Demo coming-soon publish failed tmdbId={}: {}", tmdbId, ex.getMessage());
            }
        }

        log.info("Demo seed done: +{} đang chiếu, +{} sắp chiếu (tổng ~{}/{})",
                publishedNow, publishedSoon,
                cinemaCatalogService.countNowShowingAtCinema(),
                cinemaCatalogService.countComingSoonAtCinema());

        if (publishedNow == 0 && publishedSoon == 0 && nowHave < targetNow) {
            log.warn("Không đăng thêm được phim demo — kiểm tra TMDB_API_KEY trong application-local.properties");
        }
    }

    private static void pauseBriefly() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
