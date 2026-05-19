package com.re.cinemamoviebookingsystem.tmdb.exception;

import lombok.Getter;

@Getter
public class TmdbApiException extends RuntimeException {

    private final int statusCode;

    public TmdbApiException(String message) {
        super(message);
        this.statusCode = 503;
    }

    public TmdbApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public TmdbApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 503;
    }
}
