package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.ContentArticleDto;
import com.re.cinemamoviebookingsystem.dto.response.HomeFeaturedCardDto;
import com.re.cinemamoviebookingsystem.dto.response.HomeSidebarCarouselDto;
import com.re.cinemamoviebookingsystem.dto.response.HomeSidebarSlideDto;
import com.re.cinemamoviebookingsystem.enums.ContentCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomeFeaturedContentService {

    private static final int CARDS_PER_SLIDE = 3;
    private static final int MAX_CARDS = 15;
    private static final int MIN_CARDS_FOR_CAROUSEL = 3;

    private static final List<String> HOME_PROMO_ASSETS = List.of(
            "card genz.webp",
            "lat mat 8.webp",
            "movie summer 2025.webp",
            "ky an khong dau.webp",
            "children summer 2025.webp",
            "event woman day.webp",
            "movie vietnam.webp",
            "30-4-2025.webp"
    );

    private static final List<String> HOME_FESTIVAL_ASSETS = List.of(
            "kinofest.jpg",
            "calander movie.jpg",
            "movie free.jpg",
            "lat mat 8.webp",
            "lat mat 8-1.webp",
            "lat mat 8-2.webp"
    );

    private static final List<String> HOME_NEWS_ASSETS = List.of(
            "children summer 2025.webp",
            "movie summer 2025.webp",
            "lat mat 8.webp",
            "doremon movie 45.jpg",
            "event woman day.webp",
            "ky an khong dau.webp"
    );

    private final StaticContentService staticContentService;

    public Optional<HomeFeaturedCardDto> featuredHomePromo() {
        return sidebarPromotionsCarousel()
                .flatMap(carousel -> carousel.getSlides().stream()
                        .flatMap(slide -> slide.getCards().stream())
                        .findFirst());
    }

    public Optional<HomeSidebarCarouselDto> sidebarPromotionsCarousel() {
        return buildCarousel(
                staticContentService.listPromotions(),
                ContentCategory.PROMOTION,
                HOME_PROMO_ASSETS);
    }

    public Optional<HomeSidebarCarouselDto> sidebarFestivalsCarousel() {
        return buildCarousel(
                staticContentService.listFestivals(),
                ContentCategory.FESTIVAL,
                HOME_FESTIVAL_ASSETS);
    }

    public Optional<HomeSidebarCarouselDto> sidebarNewsCarousel() {
        return buildCarousel(
                staticContentService.listNews(),
                ContentCategory.NEWS,
                HOME_NEWS_ASSETS);
    }

    private Optional<HomeSidebarCarouselDto> buildCarousel(
            List<ContentArticleDto> articles,
            ContentCategory category,
            List<String> preferredAssets) {
        List<HomeFeaturedCardDto> cards = collectCards(articles, category, preferredAssets);
        if (cards.size() < MIN_CARDS_FOR_CAROUSEL) {
            return Optional.empty();
        }
        List<HomeSidebarSlideDto> slides = chunkIntoSlides(cards);
        if (slides.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(HomeSidebarCarouselDto.builder().slides(slides).build());
    }

    private List<HomeFeaturedCardDto> collectCards(
            List<ContentArticleDto> articles,
            ContentCategory category,
            List<String> preferredAssets) {
        List<HomeFeaturedCardDto> result = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();

        for (String asset : preferredAssets) {
            for (ContentArticleDto article : articles) {
                addCardIfNew(article, category, asset, seenUrls, result);
                if (result.size() >= MAX_CARDS) {
                    return result;
                }
            }
        }

        for (ContentArticleDto article : articles) {
            addAllImagesFromArticle(article, category, seenUrls, result);
            if (result.size() >= MAX_CARDS) {
                return result;
            }
        }

        return result;
    }

    private void addAllImagesFromArticle(
            ContentArticleDto article,
            ContentCategory category,
            Set<String> seenUrls,
            List<HomeFeaturedCardDto> result) {
        if (article.getThumbnail() != null && isUsableThumbnail(article.getThumbnail())) {
            addCardIfNew(article, category, article.getThumbnail(), seenUrls, result);
        }
        if (article.getImages() == null || article.getImageUrls() == null) {
            return;
        }
        for (int i = 0; i < article.getImages().size(); i++) {
            if (i >= article.getImageUrls().size()) {
                break;
            }
            String asset = article.getImages().get(i);
            if (!isUsableThumbnail(asset)) {
                continue;
            }
            String url = article.getImageUrls().get(i);
            if (url == null || url.isBlank() || !seenUrls.add(url)) {
                continue;
            }
            result.add(HomeFeaturedCardDto.builder()
                    .id(article.getId())
                    .imageUrl(url)
                    .category(category)
                    .build());
            if (result.size() >= MAX_CARDS) {
                return;
            }
        }
    }

    private void addCardIfNew(
            ContentArticleDto article,
            ContentCategory category,
            String asset,
            Set<String> seenUrls,
            List<HomeFeaturedCardDto> result) {
        toCardIfMatches(article, category, asset).ifPresent(card -> {
            if (seenUrls.add(card.getImageUrl())) {
                result.add(card);
            }
        });
    }

    private List<HomeSidebarSlideDto> chunkIntoSlides(List<HomeFeaturedCardDto> cards) {
        int slideCount = (cards.size() + CARDS_PER_SLIDE - 1) / CARDS_PER_SLIDE;
        List<HomeSidebarSlideDto> slides = new ArrayList<>();
        for (int s = 0; s < slideCount; s++) {
            List<HomeFeaturedCardDto> slideCards = new ArrayList<>(CARDS_PER_SLIDE);
            for (int i = 0; i < CARDS_PER_SLIDE; i++) {
                slideCards.add(cards.get((s * CARDS_PER_SLIDE + i) % cards.size()));
            }
            slides.add(HomeSidebarSlideDto.builder().cards(List.copyOf(slideCards)).build());
        }
        return slides;
    }

    private boolean isUsableThumbnail(String thumbnail) {
        if (thumbnail == null || thumbnail.isBlank()) {
            return false;
        }
        String lower = thumbnail.toLowerCase();
        return !"movie animation.webp".equals(thumbnail)
                && (lower.endsWith(".webp") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"));
    }

    private Optional<HomeFeaturedCardDto> toCardIfMatches(
            ContentArticleDto article,
            ContentCategory category,
            String asset) {
        String imageUrl = resolveImageUrl(article, asset);
        if (imageUrl == null || !isUsableThumbnail(asset)) {
            return Optional.empty();
        }
        return Optional.of(HomeFeaturedCardDto.builder()
                .id(article.getId())
                .imageUrl(imageUrl)
                .category(category)
                .build());
    }

    private String resolveImageUrl(ContentArticleDto article, String asset) {
        if (asset.equals(article.getThumbnail()) && article.getThumbnailUrl() != null) {
            return article.getThumbnailUrl();
        }
        if (article.getImages() != null && article.getImageUrls() != null) {
            for (int i = 0; i < article.getImages().size(); i++) {
                if (asset.equals(article.getImages().get(i)) && i < article.getImageUrls().size()) {
                    return article.getImageUrls().get(i);
                }
            }
        }
        return null;
    }
}
