package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ContentArticleDto {
    private String id;
    private String title;
    private String date;
    private String thumbnail;
    private List<String> images = new ArrayList<>();
    private String contentHtml;
    private String thumbnailUrl;
    private List<String> imageUrls = new ArrayList<>();
}
