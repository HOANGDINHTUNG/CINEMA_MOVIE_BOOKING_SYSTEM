package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.ContentArticleDto;
import com.re.cinemamoviebookingsystem.entity.ContentArticle;
import com.re.cinemamoviebookingsystem.enums.ContentCategory;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.ContentArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentArticleService {

    private static final DateTimeFormatter FLEXIBLE_DATE = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .toFormatter();

    private final ContentArticleRepository contentArticleRepository;

    @Transactional(readOnly = true)
    public List<ContentArticleDto> list(ContentCategory category) {
        return contentArticleRepository.findByCategoryOrderByPublishedDateDescTitleAsc(category)
                .stream()
                .map(article -> toDto(article, category.getImageBase()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ContentArticleDto> find(ContentCategory category, String id) {
        return contentArticleRepository.findByCategoryAndId(category, id)
                .map(article -> toDto(article, category.getImageBase()));
    }

    @Transactional
    public void save(ContentCategory category, ContentArticleDto dto, boolean isNew) {
        if (dto.getId() == null || dto.getId().isBlank()) {
            dto.setId(category.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (isNew && contentArticleRepository.existsByCategoryAndId(category, dto.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "ID bài viết đã tồn tại");
        }

        ContentArticle article = contentArticleRepository.findByCategoryAndId(category, dto.getId())
                .orElseGet(() -> ContentArticle.builder()
                        .id(dto.getId())
                        .category(category)
                        .build());

        article.setTitle(requireTitle(dto.getTitle()));
        article.setPublishedDate(parseDate(dto.getDate()));
        article.setThumbnail(trimToNull(dto.getThumbnail()));
        article.setImages(dto.getImages() != null ? new ArrayList<>(dto.getImages()) : new ArrayList<>());
        article.setContentHtml(dto.getContentHtml());

        contentArticleRepository.save(article);
    }

    @Transactional
    public void delete(ContentCategory category, String id) {
        ContentArticle article = contentArticleRepository.findByCategoryAndId(category, id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Không tìm thấy bài viết"));
        contentArticleRepository.delete(article);
    }

    public ContentArticleDto toDto(ContentArticle article, String imageBase) {
        ContentArticleDto dto = new ContentArticleDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setDate(article.getPublishedDate() != null ? article.getPublishedDate().toString() : null);
        dto.setThumbnail(article.getThumbnail());
        dto.setImages(article.getImages() != null ? new ArrayList<>(article.getImages()) : new ArrayList<>());
        dto.setContentHtml(article.getContentHtml());
        resolveUrls(dto, imageBase);
        return dto;
    }

    public ContentArticle fromImportDto(ContentCategory category, ContentArticleDto dto) {
        return ContentArticle.builder()
                .id(dto.getId())
                .category(category)
                .title(requireTitle(dto.getTitle()))
                .publishedDate(parseDate(dto.getDate()))
                .thumbnail(trimToNull(dto.getThumbnail()))
                .images(dto.getImages() != null ? new ArrayList<>(dto.getImages()) : new ArrayList<>())
                .contentHtml(dto.getContentHtml())
                .build();
    }

    private void resolveUrls(ContentArticleDto item, String imageBase) {
        if (item.getThumbnail() != null && !item.getThumbnail().isBlank()) {
            item.setThumbnailUrl(imageBase + item.getThumbnail());
        }
        item.getImageUrls().clear();
        if (item.getImages() != null) {
            item.getImages().forEach(img -> {
                if (img != null && !img.isBlank()) {
                    item.getImageUrls().add(imageBase + img);
                }
            });
        }
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Tiêu đề không được để trống");
        }
        return title.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        String trimmed = date.trim();
        try {
            return LocalDate.parse(trimmed, FLEXIBLE_DATE);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(trimmed);
            } catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Ngày không hợp lệ: " + date);
            }
        }
    }
}
