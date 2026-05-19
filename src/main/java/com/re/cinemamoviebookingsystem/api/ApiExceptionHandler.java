package com.re.cinemamoviebookingsystem.api;

import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.tmdb.exception.TmdbApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.re.cinemamoviebookingsystem.api")
public class ApiExceptionHandler {

    @ExceptionHandler(TmdbApiException.class)
    public ResponseEntity<ApiErrorResponse> handleTmdb(TmdbApiException ex) {
        HttpStatus status = ex.getStatusCode() == 404
                ? HttpStatus.NOT_FOUND
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .status(status.value())
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(ex.getMessage() != null ? ex.getMessage() : "Lỗi hệ thống")
                .build());
    }
}
