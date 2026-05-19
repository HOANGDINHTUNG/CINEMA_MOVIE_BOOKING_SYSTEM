package com.re.cinemamoviebookingsystem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.re.cinemamoviebookingsystem.dto.response.ContentArticleDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class StaticContentService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ContentArticleDto> listPromotions() {
        return loadArticles("database/promotion.json", "/assets/img/events/");
    }

    public List<ContentArticleDto> listNews() {
        return loadArticles("database/event.json", "/assets/img/events/");
    }

    public List<ContentArticleDto> listFestivals() {
        return loadArticles("database/festival.json", "/assets/img/eventFestival/");
    }

    public Optional<ContentArticleDto> findPromotion(String id) {
        return listPromotions().stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    public Optional<ContentArticleDto> findNews(String id) {
        return listNews().stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    public Optional<ContentArticleDto> findFestival(String id) {
        return listFestivals().stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    private List<ContentArticleDto> loadArticles(String classpathFile, String imageBase) {
        try (InputStream in = new ClassPathResource(classpathFile).getInputStream()) {
            List<ContentArticleDto> items = objectMapper.readValue(in, new TypeReference<>() {});
            items.forEach(item -> resolveUrls(item, imageBase));
            return items;
        } catch (IOException e) {
            return List.of();
        }
    }

    private void resolveUrls(ContentArticleDto item, String imageBase) {
        if (item.getThumbnail() != null && !item.getThumbnail().isBlank()) {
            item.setThumbnailUrl(imageBase + item.getThumbnail());
        }
        if (item.getImages() != null) {
            item.getImages().forEach(img -> {
                if (img != null && !img.isBlank()) {
                    item.getImageUrls().add(imageBase + img);
                }
            });
        }
    }
}
