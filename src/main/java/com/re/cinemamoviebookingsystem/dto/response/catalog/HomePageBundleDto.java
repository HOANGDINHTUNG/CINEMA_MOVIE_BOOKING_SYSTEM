package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomePageBundleDto {
    private HomeSectionResultDto nowShowing;
    private HomeSectionResultDto comingSoon;
}
