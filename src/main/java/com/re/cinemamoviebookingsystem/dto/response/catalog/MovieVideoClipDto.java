package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MovieVideoClipDto {
    private String name;
    private String type;
    private String site;
    private String publishedAt;
    private String youtubeKey;
    private String thumbUrl;
    private String watchUrl;
}
