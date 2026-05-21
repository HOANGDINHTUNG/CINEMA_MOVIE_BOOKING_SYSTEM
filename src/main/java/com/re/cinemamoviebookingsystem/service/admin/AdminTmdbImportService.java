package com.re.cinemamoviebookingsystem.service.admin;

import com.re.cinemamoviebookingsystem.dto.response.AdminTmdbImportItemDto;
import com.re.cinemamoviebookingsystem.dto.response.AdminTmdbImportStateDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogPageDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogSummaryDto;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminTmdbImportService {

    private static final int HOT_MAX = 40;

    private final TmdbCatalogService tmdbCatalogService;
    private final AdminMovieCatalogService adminMovieCatalogService;

    @Transactional(readOnly = true)
    public List<AdminTmdbImportItemDto> listHotForImport(AppLanguage lang, boolean onlyNew) {
        LinkedHashMap<Long, MovieCatalogSummaryDto> merged = new LinkedHashMap<>();
        appendPage(merged, tmdbCatalogService.trending(lang, 1, "week"));
        appendPage(merged, tmdbCatalogService.nowPlaying(lang, 1));
        return enrich(new ArrayList<>(merged.values()), onlyNew, lang);
    }

    @Transactional(readOnly = true)
    public List<AdminTmdbImportItemDto> enrichSearchResults(MovieCatalogPageDto page,
                                                           AppLanguage lang,
                                                           boolean onlyNew) {
        if (page == null || page.getResults() == null) {
            return List.of();
        }
        return enrich(page.getResults(), onlyNew, lang);
    }

    private void appendPage(LinkedHashMap<Long, MovieCatalogSummaryDto> target, MovieCatalogPageDto page) {
        if (page == null || page.getResults() == null) {
            return;
        }
        for (MovieCatalogSummaryDto item : page.getResults()) {
            if (item.getTmdbId() != null && !target.containsKey(item.getTmdbId())) {
                target.put(item.getTmdbId(), item);
            }
        }
    }

    private List<AdminTmdbImportItemDto> enrich(List<MovieCatalogSummaryDto> summaries,
                                                boolean onlyNew,
                                                AppLanguage lang) {
        Map<Long, AdminTmdbImportStateDto> stateMap = adminMovieCatalogService.buildTmdbImportStateMap();
        List<AdminTmdbImportItemDto> items = new ArrayList<>();
        for (MovieCatalogSummaryDto summary : summaries) {
            if (summary.getTmdbId() == null) {
                continue;
            }
            AdminTmdbImportStateDto state = adminMovieCatalogService.stateForTmdbId(summary.getTmdbId(), stateMap);
            if (onlyNew && !state.isPublishable()) {
                continue;
            }
            items.add(AdminTmdbImportItemDto.builder()
                    .tmdb(summary)
                    .cinemaState(state)
                    .build());
            if (items.size() >= HOT_MAX) {
                break;
            }
        }
        return items;
    }
}
