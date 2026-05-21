package com.re.cinemamoviebookingsystem.tmdb.service;

import com.re.cinemamoviebookingsystem.dto.response.catalog.CastMemberDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogDetailDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogPageDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogSummaryDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieVideoClipDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.ProductionCompanyDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.TmdbGenreItemDto;
import com.re.cinemamoviebookingsystem.util.MovieAgeUtil;
import com.re.cinemamoviebookingsystem.tmdb.dto.TmdbMovieCreditsDto;
import com.re.cinemamoviebookingsystem.tmdb.client.TmdbClient;
import com.re.cinemamoviebookingsystem.tmdb.dto.TmdbGenreDto;
import com.re.cinemamoviebookingsystem.tmdb.dto.TmdbMovieDetailsDto;
import com.re.cinemamoviebookingsystem.tmdb.dto.TmdbMovieImagesDto;
import com.re.cinemamoviebookingsystem.tmdb.dto.TmdbMovieSummaryDto;
import com.re.cinemamoviebookingsystem.tmdb.dto.TmdbMovieVideosDto;
import com.re.cinemamoviebookingsystem.tmdb.dto.TmdbPagedResponseDto;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.util.TmdbImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TmdbCatalogService {

    private final TmdbClient tmdbClient;
    private final TmdbImageUrlBuilder imageUrlBuilder;

    @Cacheable(cacheNames = "tmdbDiscover", key = "#lang.tmdbCode + '-' + #page")
    public MovieCatalogPageDto discoverLatest(AppLanguage lang, int page) {
        return mapPage(tmdbClient.discoverLatest(lang.getTmdbCode(), page));
    }

    @Cacheable(cacheNames = "tmdbNowPlaying", key = "#lang.tmdbCode + '-' + #page")
    public MovieCatalogPageDto nowPlaying(AppLanguage lang, int page) {
        return mapPage(tmdbClient.nowPlaying(lang.getTmdbCode(), page));
    }

    @Cacheable(cacheNames = "tmdbUpcoming", key = "#lang.tmdbCode + '-' + #page")
    public MovieCatalogPageDto upcoming(AppLanguage lang, int page) {
        return mapPage(tmdbClient.upcoming(lang.getTmdbCode(), page));
    }

    @Cacheable(cacheNames = "tmdbTrending", key = "#lang.tmdbCode + '-' + #window + '-' + #page")
    public MovieCatalogPageDto trending(AppLanguage lang, int page, String window) {
        return mapPage(tmdbClient.trending(lang.getTmdbCode(), page, window));
    }

    @Cacheable(cacheNames = "tmdbSearch", key = "#lang.tmdbCode + '-' + #query + '-' + #page")
    public MovieCatalogPageDto search(AppLanguage lang, int page, String query) {
        return mapPage(tmdbClient.searchMovies(lang.getTmdbCode(), page, query));
    }

    @Cacheable(cacheNames = "tmdbDetail", key = "#lang.tmdbCode + '-' + #tmdbId")
    public MovieCatalogDetailDto getDetail(AppLanguage lang, long tmdbId) {
        TmdbMovieDetailsDto raw = tmdbClient.getMovieDetails(tmdbId, lang.getTmdbCode());
        return mapDetail(raw);
    }

    @Cacheable(cacheNames = "tmdbGenres", key = "#lang.tmdbCode")
    public List<TmdbGenreItemDto> listGenres(AppLanguage lang) {
        return tmdbClient.listGenres(lang.getTmdbCode()).getGenres().stream()
                .map(this::mapGenre)
                .collect(Collectors.toList());
    }

    public MovieCatalogPageDto discoverCurrentYear(AppLanguage lang, int page) {
        return mapPage(tmdbClient.discoverCurrentYear(lang.getTmdbCode(), page));
    }

    /**
     * Lấy tối đa {@code maxResults} phim TMDB có poster, overview, điểm đánh giá.
     */
    @Cacheable(cacheNames = "tmdbDiscoverLatestBatch", key = "#lang.tmdbCode + '-' + #maxResults + '-' + (#genreId != null ? #genreId : 'all')")
    public List<MovieCatalogSummaryDto> discoverLatestUpTo(AppLanguage lang, int maxResults, Integer genreId) {
        List<MovieCatalogSummaryDto> collected = new ArrayList<>();
        int maxPages = Math.max(1, (maxResults + 19) / 20);
        int page = 1;
        while (page <= maxPages && collected.size() < maxResults) {
            MovieCatalogPageDto batch = mapPage(tmdbClient.discoverQuality(lang.getTmdbCode(), page, genreId));
            if (batch.getResults() == null || batch.getResults().isEmpty()) {
                break;
            }
            for (MovieCatalogSummaryDto item : batch.getResults()) {
                if (hasQualityMetadata(item)) {
                    collected.add(item);
                }
                if (collected.size() >= maxResults) {
                    break;
                }
            }
            if (batch.getTotalPages() > 0 && page >= batch.getTotalPages()) {
                break;
            }
            page++;
        }
        return collected.stream().limit(maxResults).toList();
    }

    public List<MovieCatalogSummaryDto> discoverLatestUpTo(AppLanguage lang, int maxResults) {
        return discoverLatestUpTo(lang, maxResults, null);
    }

    @Cacheable(cacheNames = "tmdbNowPlayingBatch", key = "#lang.tmdbCode + '-' + #maxResults",
            unless = "#result == null || #result.isEmpty()")
    public List<MovieCatalogSummaryDto> nowPlayingUpTo(AppLanguage lang, int maxResults) {
        return fetchSummariesUpTo(lang, maxResults, page -> mapPage(tmdbClient.nowPlaying(lang.getTmdbCode(), page)),
                TmdbCatalogService::hasHomeListMetadata);
    }

    @Cacheable(cacheNames = "tmdbUpcomingBatch", key = "#lang.tmdbCode + '-' + #maxResults",
            unless = "#result == null || #result.isEmpty()")
    public List<MovieCatalogSummaryDto> upcomingUpTo(AppLanguage lang, int maxResults) {
        return fetchSummariesUpTo(lang, maxResults, page -> mapPage(tmdbClient.upcoming(lang.getTmdbCode(), page)),
                TmdbCatalogService::hasHomeListMetadata);
    }

    private List<MovieCatalogSummaryDto> fetchSummariesUpTo(AppLanguage lang, int maxResults,
                                                            java.util.function.IntFunction<MovieCatalogPageDto> fetchPage,
                                                            Predicate<MovieCatalogSummaryDto> filter) {
        List<MovieCatalogSummaryDto> collected = new ArrayList<>();
        int maxPages = Math.max(1, (maxResults + 19) / 20);
        int page = 1;
        while (page <= maxPages && collected.size() < maxResults) {
            MovieCatalogPageDto batch = fetchPage.apply(page);
            if (batch.getResults() == null || batch.getResults().isEmpty()) {
                break;
            }
            for (MovieCatalogSummaryDto item : batch.getResults()) {
                if (filter.test(item)) {
                    collected.add(item);
                }
                if (collected.size() >= maxResults) {
                    break;
                }
            }
            if (batch.getTotalPages() > 0 && page >= batch.getTotalPages()) {
                break;
            }
            page++;
        }
        return collected.stream().limit(maxResults).toList();
    }

    private List<MovieCatalogSummaryDto> fetchQualitySummariesUpTo(AppLanguage lang, int maxResults,
                                                                   java.util.function.IntFunction<MovieCatalogPageDto> fetchPage) {
        return fetchSummariesUpTo(lang, maxResults, fetchPage, TmdbCatalogService::hasQualityMetadata);
    }

    public Map<Integer, String> genreNameMap(AppLanguage lang) {
        return listGenres(lang).stream()
                .collect(Collectors.toMap(TmdbGenreItemDto::getId, TmdbGenreItemDto::getName, (a, b) -> a));
    }

    public static boolean hasQualityMetadata(MovieCatalogSummaryDto item) {
        if (item == null || item.getTmdbId() == null) {
            return false;
        }
        boolean hasPoster = item.getPosterUrl() != null && !item.getPosterUrl().isBlank();
        boolean hasOverview = item.getOverview() != null && !item.getOverview().isBlank();
        boolean hasRating = item.getVoteAverage() != null && item.getVoteAverage() > 0;
        boolean hasVotes = item.getVoteCount() != null && item.getVoteCount() >= 50;
        return hasPoster && hasOverview && hasRating && hasVotes;
    }

    /** Bộ lọc nhẹ hơn cho now_playing / upcoming (nhiều phim chưa đủ 50 vote trên TMDB). */
    public static boolean hasHomeListMetadata(MovieCatalogSummaryDto item) {
        if (item == null || item.getTmdbId() == null) {
            return false;
        }
        boolean hasPoster = item.getPosterUrl() != null && !item.getPosterUrl().isBlank();
        boolean hasTitle = item.getTitle() != null && !item.getTitle().isBlank();
        return hasPoster && hasTitle;
    }

    private MovieCatalogPageDto mapPage(TmdbPagedResponseDto<TmdbMovieSummaryDto> page) {
        List<MovieCatalogSummaryDto> results = page.getResults() == null
                ? List.of()
                : page.getResults().stream().map(this::mapSummary).collect(Collectors.toList());
        return MovieCatalogPageDto.builder()
                .page(page.getPage())
                .totalPages(page.getTotalPages())
                .totalResults(page.getTotalResults())
                .results(results)
                .build();
    }

    private MovieCatalogSummaryDto mapSummary(TmdbMovieSummaryDto item) {
        return MovieCatalogSummaryDto.builder()
                .tmdbId(item.getId())
                .title(item.getTitle())
                .originalTitle(item.getOriginalTitle())
                .overview(item.getOverview())
                .posterUrl(imageUrlBuilder.posterW500(item.getPosterPath()))
                .backdropUrl(imageUrlBuilder.backdropOriginal(item.getBackdropPath()))
                .releaseDate(item.getReleaseDate())
                .voteAverage(item.getVoteAverage())
                .voteCount(item.getVoteCount())
                .popularity(item.getPopularity())
                .genreIds(item.getGenreIds())
                .build();
    }

    private MovieCatalogDetailDto mapDetail(TmdbMovieDetailsDto raw) {
        String title = raw.getTitle();
        List<MovieVideoClipDto> youtubeClips = mapVideoClips(raw.getVideos());
        List<CastMemberDto> castMembers = mapCast(raw.getCredits());
        return MovieCatalogDetailDto.builder()
                .tmdbId(raw.getId())
                .title(title)
                .originalTitle(raw.getOriginalTitle())
                .overview(raw.getOverview())
                .posterUrl(imageUrlBuilder.posterW500(raw.getPosterPath()))
                .posterPath(raw.getPosterPath())
                .backdropUrl(imageUrlBuilder.backdropOriginal(raw.getBackdropPath()))
                .releaseDate(raw.getReleaseDate())
                .voteAverage(raw.getVoteAverage())
                .voteCount(raw.getVoteCount())
                .runtime(raw.getRuntime())
                .tagline(raw.getTagline())
                .status(raw.getStatus())
                .imdbId(raw.getImdbId())
                .trailerUrl(resolveTrailer(raw.getVideos()))
                .trailerEmbedUrl(resolveTrailerEmbedUrl(raw.getVideos()))
                .trailerYoutubeKey(resolveTrailerKey(raw.getVideos()))
                .genres(raw.getGenres() == null ? List.of() : raw.getGenres().stream().map(this::mapGenre).collect(Collectors.toList()))
                .cast(castMembers)
                .castTotalCount(castMembers.size())
                .videoClips(youtubeClips)
                .videoClipTotalCount(countYoutubeVideos(raw.getVideos()))
                .backdropGalleryUrls(mapBackdropGallery(raw.getImages()))
                .posterGalleryUrls(mapPosterGallery(raw.getImages()))
                .logoGalleryUrls(mapLogoGallery(raw.getImages()))
                .similarMovies(mapMovieRow(raw.getSimilar(), 20))
                .recommendedMovies(mapMovieRow(raw.getRecommendations(), 20))
                .director(mapDirector(raw.getCredits()))
                .writers(mapWriters(raw.getCredits()))
                .productionCompanies(mapProductionCompanies(raw.getProductionCompanies()))
                .ageLabel(MovieAgeUtil.extractAgeLabel(title))
                .build();
    }

    private String mapDirector(TmdbMovieCreditsDto credits) {
        if (credits == null || credits.getCrew() == null) {
            return null;
        }
        return credits.getCrew().stream()
                .filter(c -> c.getJob() != null && "Director".equalsIgnoreCase(c.getJob()))
                .map(TmdbMovieCreditsDto.TmdbCrewMemberDto::getName)
                .findFirst()
                .orElse(null);
    }

    private List<String> mapWriters(TmdbMovieCreditsDto credits) {
        if (credits == null || credits.getCrew() == null) {
            return List.of();
        }
        return credits.getCrew().stream()
                .filter(c -> c.getJob() != null && (
                        "Writer".equalsIgnoreCase(c.getJob())
                                || "Screenplay".equalsIgnoreCase(c.getJob())
                                || "Story".equalsIgnoreCase(c.getJob())))
                .map(TmdbMovieCreditsDto.TmdbCrewMemberDto::getName)
                .distinct()
                .limit(4)
                .collect(Collectors.toList());
    }

    private List<ProductionCompanyDto> mapProductionCompanies(List<TmdbMovieDetailsDto.TmdbProductionCompanyDto> companies) {
        if (companies == null) {
            return List.of();
        }
        return companies.stream()
                .filter(c -> c.getName() != null && !c.getName().isBlank())
                .limit(8)
                .map(c -> ProductionCompanyDto.builder()
                        .name(c.getName())
                        .logoUrl(imageUrlBuilder.build("w92", c.getLogoPath()))
                        .originCountry(c.getOriginCountry())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CastMemberDto> mapCast(TmdbMovieCreditsDto credits) {
        if (credits == null || credits.getCast() == null) {
            return List.of();
        }
        return credits.getCast().stream()
                .filter(c -> c.getName() != null)
                .sorted(Comparator.comparing(c -> c.getOrder() != null ? c.getOrder() : 999))
                .limit(80)
                .map(c -> CastMemberDto.builder()
                        .personId(c.getId())
                        .name(c.getName())
                        .character(c.getCharacter())
                        .profileUrl(imageUrlBuilder.build("w185", c.getProfilePath()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<MovieVideoClipDto> mapVideoClips(TmdbMovieVideosDto videos) {
        if (videos == null || videos.getResults() == null) {
            return List.of();
        }
        return videos.getResults().stream()
                .filter(v -> v.getKey() != null && !v.getKey().isBlank()
                        && "YouTube".equalsIgnoreCase(v.getSite()))
                .map(v -> {
                    String key = v.getKey();
                    String label = v.getName();
                    if (label == null || label.isBlank()) {
                        label = v.getType() != null ? v.getType() : "Video";
                    }
                    return MovieVideoClipDto.builder()
                            .name(label)
                            .type(v.getType())
                            .site(v.getSite())
                            .publishedAt(v.getPublishedAt())
                            .youtubeKey(key)
                            .thumbUrl("https://img.youtube.com/vi/" + key + "/mqdefault.jpg")
                            .watchUrl("https://www.youtube.com/watch?v=" + key)
                            .build();
                })
                .limit(40)
                .collect(Collectors.toList());
    }

    private static int countYoutubeVideos(TmdbMovieVideosDto videos) {
        if (videos == null || videos.getResults() == null) {
            return 0;
        }
        return (int) videos.getResults().stream()
                .filter(v -> v.getKey() != null && !v.getKey().isBlank()
                        && "YouTube".equalsIgnoreCase(v.getSite()))
                .count();
    }

    private List<String> mapBackdropGallery(TmdbMovieImagesDto images) {
        if (images == null || images.getBackdrops() == null) {
            return List.of();
        }
        return images.getBackdrops().stream()
                .map(TmdbMovieImagesDto.TmdbImageFileDto::getFilePath)
                .filter(p -> p != null && !p.isBlank())
                .limit(18)
                .map(p -> imageUrlBuilder.build("w780", p))
                .collect(Collectors.toList());
    }

    private List<String> mapPosterGallery(TmdbMovieImagesDto images) {
        if (images == null || images.getPosters() == null) {
            return List.of();
        }
        return images.getPosters().stream()
                .map(TmdbMovieImagesDto.TmdbImageFileDto::getFilePath)
                .filter(p -> p != null && !p.isBlank())
                .limit(18)
                .map(p -> imageUrlBuilder.build("w342", p))
                .collect(Collectors.toList());
    }

    private List<String> mapLogoGallery(TmdbMovieImagesDto images) {
        if (images == null || images.getLogos() == null) {
            return List.of();
        }
        return images.getLogos().stream()
                .map(TmdbMovieImagesDto.TmdbImageFileDto::getFilePath)
                .filter(p -> p != null && !p.isBlank())
                .limit(12)
                .map(p -> imageUrlBuilder.build("w300", p))
                .collect(Collectors.toList());
    }

    private List<MovieCatalogSummaryDto> mapMovieRow(TmdbPagedResponseDto<TmdbMovieSummaryDto> page, int limit) {
        if (page == null || page.getResults() == null) {
            return List.of();
        }
        return page.getResults().stream()
                .limit(limit)
                .map(this::mapSummary)
                .collect(Collectors.toList());
    }

    private TmdbGenreItemDto mapGenre(TmdbGenreDto genre) {
        return TmdbGenreItemDto.builder()
                .id(genre.getId())
                .name(genre.getName())
                .build();
    }

    public static String resolveTrailer(TmdbMovieVideosDto videos) {
        String key = resolveTrailerKey(videos);
        return key != null ? "https://www.youtube.com/watch?v=" + key : null;
    }

    public static String resolveTrailerEmbedUrl(TmdbMovieVideosDto videos) {
        String key = resolveTrailerKey(videos);
        return key != null ? "https://www.youtube.com/embed/" + key : null;
    }

    private static String resolveTrailerKey(TmdbMovieVideosDto videos) {
        if (videos == null || videos.getResults() == null) {
            return null;
        }
        var list = videos.getResults();
        return list.stream()
                .filter(v -> "YouTube".equalsIgnoreCase(v.getSite()) && "Trailer".equalsIgnoreCase(v.getType()) && Boolean.TRUE.equals(v.getOfficial()))
                .findFirst()
                .or(() -> list.stream().filter(v -> "YouTube".equalsIgnoreCase(v.getSite()) && "Trailer".equalsIgnoreCase(v.getType())).findFirst())
                .or(() -> list.stream().filter(v -> "YouTube".equalsIgnoreCase(v.getSite())).findFirst())
                .map(TmdbMovieVideosDto.TmdbVideoItemDto::getKey)
                .orElse(null);
    }
}
