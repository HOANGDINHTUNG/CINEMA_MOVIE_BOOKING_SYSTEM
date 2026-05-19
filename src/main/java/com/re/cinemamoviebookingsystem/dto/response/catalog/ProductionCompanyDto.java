package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductionCompanyDto {
    private String name;
    private String logoUrl;
    private String originCountry;
}
