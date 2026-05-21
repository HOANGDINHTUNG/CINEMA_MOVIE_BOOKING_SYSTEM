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
 * Đồng bộ catalog TMDB vào DB khi khởi động:
 * <ul>
 *   <li>{@link DemoTmdbCatalog#WAITING_SCHEDULE_TMDB_IDS} — now_playing, không suất → «Đang đợi lịch chiếu»</li>
 *   <li>{@link DemoTmdbCatalog#DEMO_SCHEDULED_TMDB_IDS} — id riêng, có lịch mẫu (nếu target &gt; 0)</li>
 * </ul>
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

        int waitingBefore = cinemaCatalogService.countPublishedWithoutShowtimes();
        int nowBefore = cinemaCatalogService.countNowShowingAtCinema();

        log.info(
                "Demo seed: waiting(now_playing)={} id, scheduled={} id. Hiện có: đợi lịch={}, có suất={}",
                DemoTmdbCatalog.WAITING_SCHEDULE_TMDB_IDS.size(),
                DemoTmdbCatalog.DEMO_SCHEDULED_TMDB_IDS.size(),
                waitingBefore,
                nowBefore
        );

        AppLanguage lang = AppLanguage.VI_VN;
        int publishedWaiting = syncCatalog(DemoTmdbCatalog.WAITING_SCHEDULE_TMDB_IDS, lang, false);
        int publishedScheduled = 0;
        if (cinemaProperties.getDemoSeedScheduledTarget() > 0) {
            publishedScheduled = syncCatalog(DemoTmdbCatalog.DEMO_SCHEDULED_TMDB_IDS, lang, true);
        } else {
            log.info("Demo seed: bỏ qua đăng phim có lịch mẫu (cinema.demo-seed-scheduled-target=0)");
        }

        int waitingAfter = cinemaCatalogService.countPublishedWithoutShowtimes();
        int nowAfter = cinemaCatalogService.countNowShowingAtCinema();

        log.info(
                "Demo seed xong: +{} đợi lịch (now_playing), +{} có lịch mẫu. Tổng: đợi lịch={}/{}, có suất={}/{}",
                publishedWaiting,
                publishedScheduled,
                waitingAfter,
                cinemaProperties.getDemoSeedWaitingTarget(),
                nowAfter,
                cinemaProperties.getDemoSeedScheduledTarget()
        );

        if (publishedWaiting == 0 && publishedScheduled == 0
                && waitingAfter < cinemaProperties.getDemoSeedWaitingTarget()) {
            log.warn(
                    "Không đăng thêm phim — kiểm tra TMDB_API_KEY và log 'publish failed' phía trên");
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
