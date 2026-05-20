package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.exception.TmdbApiException;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TmdbHealthService {

    private final TmdbCatalogService tmdbCatalogService;

    public TmdbTestResult testConnection() {
        try {
            var page = tmdbCatalogService.search(AppLanguage.VI_VN, 1, "cinema");
            int count = page.getResults() != null ? page.getResults().size() : 0;
            return new TmdbTestResult(true, "Kết nối TMDB thành công. Tìm thấy " + count + " kết quả mẫu.");
        } catch (TmdbApiException ex) {
            return new TmdbTestResult(false, ex.getMessage());
        } catch (Exception ex) {
            return new TmdbTestResult(false, "Lỗi: " + ex.getMessage());
        }
    }

    public record TmdbTestResult(boolean ok, String message) {
    }
}
