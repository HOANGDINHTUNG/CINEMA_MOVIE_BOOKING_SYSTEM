package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.ContentArticleDto;
import com.re.cinemamoviebookingsystem.enums.ContentCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaticContentService {

    private final ContentArticleService contentArticleService;

    public List<ContentArticleDto> listPromotions() {
        return contentArticleService.list(ContentCategory.PROMOTION);
    }

    public List<ContentArticleDto> listNews() {
        return contentArticleService.list(ContentCategory.NEWS);
    }

    public List<ContentArticleDto> listFestivals() {
        return contentArticleService.list(ContentCategory.FESTIVAL);
    }

    public Optional<ContentArticleDto> findPromotion(String id) {
        return contentArticleService.find(ContentCategory.PROMOTION, id);
    }

    public Optional<ContentArticleDto> findNews(String id) {
        return contentArticleService.find(ContentCategory.NEWS, id);
    }

    public Optional<ContentArticleDto> findFestival(String id) {
        return contentArticleService.find(ContentCategory.FESTIVAL, id);
    }
}
