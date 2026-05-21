package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class HomeSidebarSlideDto {
    List<HomeFeaturedCardDto> cards;
}
