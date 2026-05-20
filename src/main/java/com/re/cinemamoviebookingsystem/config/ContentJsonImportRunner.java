package com.re.cinemamoviebookingsystem.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.re.cinemamoviebookingsystem.dto.response.ContentArticleDto;
import com.re.cinemamoviebookingsystem.entity.ContentArticle;
import com.re.cinemamoviebookingsystem.enums.ContentCategory;
import com.re.cinemamoviebookingsystem.repository.ContentArticleRepository;
import com.re.cinemamoviebookingsystem.service.ContentArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * Dự phòng khi bảng content_articles trống (chưa chạy seed.sql).
 * Dữ liệu chính: {@code db/seed.sql} (sinh từ database/*.json).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentJsonImportRunner implements ApplicationRunner {

    private final ContentArticleRepository contentArticleRepository;
    private final ContentArticleService contentArticleService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (contentArticleRepository.count() > 0) {
            return;
        }
        int imported = 0;
        for (ContentCategory category : ContentCategory.values()) {
            imported += importCategory(category);
        }
        if (imported > 0) {
            log.info("Đã import {} bài nội dung từ JSON vào bảng content_articles", imported);
        }
    }

    private int importCategory(ContentCategory category) {
        String classpathFile = category.seedClasspathFile();
        try (InputStream in = new ClassPathResource(classpathFile).getInputStream()) {
            List<ContentArticleDto> items = objectMapper.readValue(in, new TypeReference<>() {});
            if (items == null || items.isEmpty()) {
                return 0;
            }
            for (ContentArticleDto dto : items) {
                if (dto.getId() == null || dto.getId().isBlank()) {
                    continue;
                }
                if (contentArticleRepository.existsById(dto.getId())) {
                    continue;
                }
                ContentArticle article = contentArticleService.fromImportDto(category, dto);
                contentArticleRepository.save(article);
            }
            return items.size();
        } catch (Exception e) {
            log.warn("Không import được {}: {}", classpathFile, e.getMessage());
            return 0;
        }
    }
}
