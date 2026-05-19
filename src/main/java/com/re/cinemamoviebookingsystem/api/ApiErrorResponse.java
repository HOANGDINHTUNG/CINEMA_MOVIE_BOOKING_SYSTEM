package com.re.cinemamoviebookingsystem.api;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiErrorResponse {
    private String message;
    private int status;
}
