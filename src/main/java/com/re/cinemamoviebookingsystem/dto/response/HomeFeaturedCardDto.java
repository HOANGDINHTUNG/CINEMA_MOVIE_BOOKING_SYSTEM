package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.ContentCategory;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HomeFeaturedCardDto {
    String id;
    String imageUrl;
    ContentCategory category;

    public String getLinkPath() {
        return switch (category) {
            case PROMOTION -> "/customer/promotions/" + id;
            case NEWS -> "/customer/news/" + id;
            case FESTIVAL -> "/customer/festival/" + id;
        };
    }
}
