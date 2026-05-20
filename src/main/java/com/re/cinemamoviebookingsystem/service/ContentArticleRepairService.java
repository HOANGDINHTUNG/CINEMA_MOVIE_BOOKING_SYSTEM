package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.enums.ContentCategory;
import com.re.cinemamoviebookingsystem.repository.ContentArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sửa dữ liệu content_articles khi seed cũ dùng trùng id news001 cho cả PROMOTION/NEWS/FESTIVAL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentArticleRepairService {

    private final ContentArticleRepository contentArticleRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void repairMissingNewsAndFestival() {
        long newsCount = contentArticleRepository
                .findByCategoryOrderByPublishedDateDescTitleAsc(ContentCategory.NEWS)
                .size();
        long festivalCount = contentArticleRepository
                .findByCategoryOrderByPublishedDateDescTitleAsc(ContentCategory.FESTIVAL)
                .size();
        boolean hasLegacyPromotionIds = contentArticleRepository
                .findByCategoryOrderByPublishedDateDescTitleAsc(ContentCategory.PROMOTION)
                .stream()
                .anyMatch(article -> article.getId() != null && article.getId().startsWith("news"));

        if (newsCount > 0 && festivalCount > 0 && !hasLegacyPromotionIds) {
            return;
        }

        int renamed = jdbcTemplate.update(
                "UPDATE content_articles a "
                        + "LEFT JOIN content_articles b ON b.id = CONCAT('promo', SUBSTRING(a.id, 5)) "
                        + "SET a.id = CONCAT('promo', SUBSTRING(a.id, 5)) "
                        + "WHERE a.category = 'PROMOTION' AND a.id LIKE 'news%' AND b.id IS NULL");
        int removedDupes = jdbcTemplate.update(
                "DELETE a FROM content_articles a "
                        + "INNER JOIN content_articles b "
                        + "ON b.id = CONCAT('promo', SUBSTRING(a.id, 5)) AND b.category = 'PROMOTION' "
                        + "WHERE a.category = 'PROMOTION' AND a.id LIKE 'news%'");
        if (renamed > 0 || removedDupes > 0) {
            log.info("PROMOTION legacy: đổi id {} bài, xóa trùng {} bài", renamed, removedDupes);
        }

        List<String> insertLines = loadNewsAndFestivalInsertLines();
        int inserted = 0;
        for (String line : insertLines) {
            try {
                jdbcTemplate.execute(line);
                inserted++;
            } catch (Exception ex) {
                log.debug("Bỏ qua INSERT content (có thể đã tồn tại): {}", ex.getMessage());
            }
        }

        long newsAfter = contentArticleRepository
                .findByCategoryOrderByPublishedDateDescTitleAsc(ContentCategory.NEWS)
                .size();
        long festivalAfter = contentArticleRepository
                .findByCategoryOrderByPublishedDateDescTitleAsc(ContentCategory.FESTIVAL)
                .size();

        log.info(
                "Content repair xong: NEWS={}, FESTIVAL={} (đã chạy {} dòng INSERT)",
                newsAfter,
                festivalAfter,
                inserted
        );
    }

    private List<String> loadNewsAndFestivalInsertLines() {
        try {
            ClassPathResource seed = new ClassPathResource("db/seed.sql");
            String seedText = new String(seed.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return seedText.lines()
                    .filter(line -> line.contains("INSERT IGNORE INTO content_articles"))
                    .filter(line -> line.contains("'NEWS'") || line.contains("'FESTIVAL'"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Không đọc được NEWS/FESTIVAL từ seed.sql: {}", e.getMessage());
            return List.of();
        }
    }
}
