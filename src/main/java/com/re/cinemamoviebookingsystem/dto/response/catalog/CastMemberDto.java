package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CastMemberDto {
    private Long personId;
    private String name;
    private String character;
    private String profileUrl;
}
