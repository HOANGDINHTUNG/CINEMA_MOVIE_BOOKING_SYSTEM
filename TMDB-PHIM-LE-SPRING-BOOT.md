# TMDB API – Phim lẻ (Movie) – Tích hợp Spring Boot

Tài liệu tổng hợp từ dự án **MOIVEZONE** (`client/src`), mô tả cách gọi API, kiểu dữ liệu (interface/DTO) và hướng dẫn áp dụng vào **Spring Boot**.

**Phạm vi tài liệu:**

| Bao gồm | Loại trừ |
|---------|----------|
| Phim lẻ (`/movie/*`, `/discover/movie`, `/trending/movie`, `/search/movie`) | Phim bộ / TV (`/tv/*`, `/discover/tv`, …) |
| Phim **mới** (discover theo ngày phát hành, now playing, upcoming, trending) | Danh sách phim **cũ** kiểu `top_rated` (nhiều kinh điển) |
| Ngôn ngữ **tiếng Việt** (`vi-VN`) và **tiếng Anh** (`en-US`) | — |
| Diễn viên, collection, genre, cấu hình ảnh (phục vụ trang chi tiết phim) | Account TMDB, rating người dùng |

---

## Mục lục

1. [Tổng quan kiến trúc trong MOIVEZONE](#1-tổng-quan-kiến-trúc-trong-moivezone)
2. [Xác thực & cấu hình HTTP](#2-xác-thực--cấu-hình-http)
3. [Ngôn ngữ en-US / vi-VN](#3-ngôn-ngữ-en-us--vi-vn)
4. [Ảnh poster / backdrop](#4-ảnh-poster--backdrop)
5. [Chiến lược chỉ lấy phim mới](#5-chiến-lược-chỉ-lấy-phim-mới)
6. [Danh sách endpoint phim lẻ](#6-danh-sách-endpoint-phim-lẻ)
7. [Interface / DTO đầy đủ](#7-interface--dto-đầy-đủ)
8. [Chi tiết phim – `append_to_response`](#8-chi-tiết-phim--append_to_response)
9. [Tích hợp Spring Boot](#9-tích-hợp-spring-boot)
10. [Bảng tham chiếu nhanh](#10-bảng-tham-chiếu-nhanh)

---

## 1. Tổng quan kiến trúc trong MOIVEZONE

### 1.1. Nguồn dữ liệu

- **API:** [The Movie Database (TMDB) v3](https://api.themoviedb.org/3)
- **Base URL:** `https://api.themoviedb.org/3`
- **Client HTTP:** Axios (`client/src/app/axiosTMDB.ts`)
- **Lớp gọi API:** `client/src/api/movie/TMDBMovie.api.ts`, `TMDBDiscover.api.ts`, `TMDBTrending.api.ts`, `TMDBSearch.api.ts`, …
- **Kiểu TypeScript:** `client/src/module/movies/database/interface/movie.ts`, `movieLists.ts`, …

### 1.2. Luồng dữ liệu điển hình

```
Spring Boot / React
    → HTTP GET + api_key (hoặc Bearer token)
    → https://api.themoviedb.org/3/...
    → JSON
    → Map sang DTO (Java) hoặc interface (TS)
    → Ghép URL ảnh: https://image.tmdb.org/t/p/{size}{file_path}
```

### 1.3. File tham chiếu trong repo

| File | Vai trò |
|------|---------|
| `client/src/app/axiosTMDB.ts` | Base URL, header, `api_key` |
| `client/src/api/movie/TMDBMovie.api.ts` | Toàn bộ endpoint `/movie/*` |
| `client/src/api/movie/TMDBDiscover.api.ts` | Discover phim mới (`primary_release_date.desc`) |
| `client/src/api/movie/TMDBTrending.api.ts` | Trending phim |
| `client/src/api/movie/TMDBSearch.api.ts` | Tìm kiếm phim |
| `client/src/module/movies/store/languageSlice.ts` | `vi-VN` \| `en-US` |
| `client/src/hooks/useFetch.ts` | Gắn `language` vào mọi list |
| `client/src/hooks/useFetchDetails.ts` | Chi tiết + `append_to_response` |
| `client/src/module/movies/pages/AllMoviesPage.tsx` | Trang “tất cả phim” – discover mới nhất |
| `client/src/module/movies/pages/DetailsPage.tsx` | Trang chi tiết – 1 request gộp nhiều sub-resource |
| `client/src/constants/constants.ts` | `IMAGE_BASE` |

---

## 2. Xác thực & cấu hình HTTP

### 2.1. Hai cách xác thực (dự án dùng cả hai)

MOIVEZONE đọc từ biến môi trường:

- `VITE_TMDB_V3_KEY` → query param `api_key`
- `VITE_TMDB_V4_TOKEN` → header `Authorization: Bearer {token}`

**Spring Boot (`application.yml`):**

```yaml
tmdb:
  base-url: https://api.themoviedb.org/3
  api-key: ${TMDB_API_KEY}          # API Key v3
  bearer-token: ${TMDB_BEARER_TOKEN} # JWT v4 (tùy chọn)
  timeout-ms: 8000
```

> **Lưu ý bảo mật:** Không commit key thật vào Git. Dùng biến môi trường hoặc secret manager.

### 2.2. Header mỗi request (theo `axiosTMDB.ts`)

```
Accept: application/json
Content-Type: application/json;charset=utf-8
Authorization: Bearer {token}   // nếu có token v4
```

Query (luôn gắn nếu có API key v3):

```
api_key={VITE_TMDB_V3_KEY}
```

### 2.3. Timeout

Dự án React: **8000 ms**. Nên giữ tương tự ở RestTemplate/WebClient.

---

## 3. Ngôn ngữ en-US / vi-VN

### 3.1. Giá trị dùng trong app

```typescript
// client/src/module/movies/store/languageSlice.ts
export type AppLanguage = "vi-VN" | "en-US";
```

| Mã app | Ý nghĩa | Khi nào dùng |
|--------|---------|--------------|
| `vi-VN` | Tiếng Việt | Mặc định trong `TMDBMovie.api.ts` |
| `en-US` | Tiếng Anh | Search mặc định `en-US` trong `searchSlice` |

### 3.2. Cách truyền lên TMDB

Tham số query **`language`** trên hầu hết endpoint:

```
GET /movie/popular?api_key=...&language=vi-VN&page=1
GET /movie/123?api_key=...&language=en-US
```

**Ảnh hưởng:** `title`, `overview`, `tagline`, tên genre (khi gọi kèm language), v.v.  
**Không đổi:** `id`, `poster_path`, `release_date`, `vote_average`.

### 3.3. Bản dịch đầy đủ (mọi ngôn ngữ)

Endpoint riêng (không lọc EN/VI):

```
GET /movie/{id}/translations
```

Dùng khi cần hiển thị song ngữ hoặc chọn bản dịch theo `iso_639_1` (`en`, `vi`).

### 3.4. Gợi ý Spring Boot

```java
public enum AppLanguage {
    VI_VN("vi-VN"),
    EN_US("en-US");

    private final String tmdbCode;
    // getter...
}
```

Truyền `language` vào mọi method service list/detail.

---

## 4. Ảnh poster / backdrop

### 4.1. Base URL

```typescript
// client/src/constants/constants.ts
export const IMAGE_BASE = "https://image.tmdb.org/t/p";
```

App còn gọi `GET /configuration` lấy `images.secure_base_url` (thường `https://image.tmdb.org/t/p/`).

### 4.2. Công thức URL đầy đủ

```
{base_url}{size}{file_path}
```

Ví dụ:

```
https://image.tmdb.org/t/p/w500/8abc123.jpg
```

`file_path` từ API luôn bắt đầu bằng `/` (vd: `/8abc123.jpg`).

### 4.3. Kích thước hay dùng trong MOIVEZONE

| Size | Dùng cho |
|------|----------|
| `w185` | Poster nhỏ (banner phụ) |
| `w300` | Poster card |
| `w500` | Poster medium (mặc định sau config) |
| `original` | Backdrop hero |

### 4.4. Configuration response (rút gọn)

```json
{
  "images": {
    "base_url": "http://image.tmdb.org/t/p/",
    "secure_base_url": "https://image.tmdb.org/t/p/",
    "poster_sizes": ["w92", "w154", "w185", "w342", "w500", "w780", "original"],
    "backdrop_sizes": ["w300", "w780", "w1280", "original"]
  }
}
```

**Spring Boot:** cache `/configuration` vài giờ; build URL ảnh ở service layer.

---

## 5. Chiến lược chỉ lấy phim mới

Dự án **không** dùng một endpoint “latest list” duy nhất. Kết hợp như sau:

### 5.1. Nguồn chính – Discover (phim mới theo ngày công chiếu)

**File:** `TMDBDiscover.api.ts` → `discoverMovies()`

```
GET /discover/movie
  ?page=1
  &language=vi-VN
  &sort_by=primary_release_date.desc
  &include_adult=false
  &include_video=false
```

**Trang All Movies** (`AllMoviesPage.tsx`): gọi 2 page TMDB / 1 page UI, sort client:

1. `release_date` giảm dần
2. `popularity` giảm dần

### 5.2. Lọc theo năm phát hành (spotlight mới)

**File:** `ExploreBackdropHeader.tsx` (chỉ phần movie)

```
GET /discover/movie
  ?sort_by=primary_release_date.desc
  &primary_release_year=2025
  &page=1
  &vote_count.gte=50
```

Đổi `2025` → năm hiện tại khi triển khai Spring Boot.

### 5.3. Phim đang chiếu / sắp chiếu

| Endpoint | Ý nghĩa | Phù hợp “phim mới” |
|----------|---------|-------------------|
| `GET /movie/now_playing` | Đang chiếu rạp | Có |
| `GET /movie/upcoming` | Sắp ra mắt | Có |
| `GET /trending/movie/day` hoặc `/week` | Xu hướng | Có (banner Home) |

### 5.4. Nên tránh khi chỉ cần phim mới

| Endpoint | Lý do |
|----------|-------|
| `GET /movie/top_rated` | Nhiều phim cũ điểm cao |
| `GET /movie/popular` | Popular tổng thể, không chỉ mới |
| `GET /discover/movie?sort_by=popularity.desc` | ExplorePage dùng cho TV/movie chung – dễ lẫn phim cũ hot |

### 5.5. Tham số Discover bổ sung (TMDB – dùng trên Spring Boot)

```
primary_release_date.gte=2024-01-01
primary_release_date.lte=2026-12-31
with_original_language=en|vi
region=VN
vote_average.gte=6
```

Ví dụ **chỉ phim từ 2024 trở đi**:

```
GET /discover/movie?sort_by=primary_release_date.desc&primary_release_date.gte=2024-01-01&language=vi-VN
```

### 5.6. Endpoint `GET /movie/latest`

Trả về **một** phim vừa được TMDB cập nhật – không phải danh sách. Ít dùng trong UI list.

---

## 6. Danh sách endpoint phim lẻ

Base: `https://api.themoviedb.org/3`  
Tất cả ví dụ dưới đây cần thêm `api_key` (và tùy chọn `language`).

### 6.1. Danh sách / khám phá

#### A. Discover – phim mới (ưu tiên)

| Method | Path | Query chính | Response type (TS) |
|--------|------|-------------|-------------------|
| GET | `/discover/movie` | `page`, `language`, `sort_by=primary_release_date.desc`, `include_adult`, `include_video`, `primary_release_year`, `vote_count.gte` | `TMDBPaginatedResponse<TMDBMovieSummary>` |

#### B. Now playing / Upcoming

| Method | Path | Query | Response |
|--------|------|-------|----------|
| GET | `/movie/now_playing` | `page`, `language` | `TMDBNowPlayingResponse` (+ `dates`) |
| GET | `/movie/upcoming` | `page`, `language` | `TMDBUpcomingResponse` (+ `dates`) |

`dates.minimum` / `dates.maximum`: khoảng ngày phát hành TMDB áp dụng cho list.

#### C. Trending phim

| Method | Path | Query | Response |
|--------|------|-------|----------|
| GET | `/trending/movie/day` | `page`, `language` | `TMDBTrendingMoviesResponse` |
| GET | `/trending/movie/week` | `page`, `language` | `TMDBTrendingMoviesResponse` |

Home banner (`App.tsx`): `trending/movie/week` + `language`.

#### D. Tìm kiếm phim

| Method | Path | Query | Response |
|--------|------|-------|----------|
| GET | `/search/movie` | `query`, `page`, `language`, `include_adult` | `TMDBSearchMovieResponse` |

### 6.2. Chi tiết & sub-resource

| Method | Path | Ghi chú |
|--------|------|---------|
| GET | `/movie/{id}` | Chi tiết; nên dùng `append_to_response` (mục 8) |
| GET | `/movie/{id}/credits` | Cast & crew |
| GET | `/movie/{id}/videos` | Trailer YouTube (`key`) |
| GET | `/movie/{id}/images` | Poster/backdrop/logo |
| GET | `/movie/{id}/keywords` | Từ khóa |
| GET | `/movie/{id}/reviews` | Đánh giá cộng đồng |
| GET | `/movie/{id}/similar` | Phim tương tự |
| GET | `/movie/{id}/recommendations` | Gợi ý |
| GET | `/movie/{id}/release_dates` | Ngày chiếu + certification theo quốc gia |
| GET | `/movie/{id}/watch/providers` | Nền tảng xem (VN trong `results.VN`) |
| GET | `/movie/{id}/translations` | Mọi bản dịch title/overview |
| GET | `/movie/{id}/alternative_titles` | Tên khác |
| GET | `/movie/{id}/external_ids` | imdb_id, … |
| GET | `/movie/{id}/lists` | Lists TMDB chứa phim |
| GET | `/movie/{id}/changes` | Lịch sử thay đổi metadata |

### 6.3. Hỗ trợ (không phải TV)

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/genre/movie/list` | Thể loại phim (`language`) |
| GET | `/configuration` | URL ảnh, sizes |
| GET | `/certification/movie/list` | Bảng rating theo quốc gia |
| GET | `/collection/{id}` | Bộ phim (Marvel, …) – `parts[]` là các **movie** |
| GET | `/person/{id}` | Diễn viên |
| GET | `/person/{id}/movie_credits` | Phim đã tham gia (lọc `media_type=movie` ở client) |

---

## 7. Interface / DTO đầy đủ

### 7.1. Phân trang chung

```typescript
interface TMDBPaginatedResponse<T> {
  page: number;
  results: T[];
  total_pages: number;
  total_results: number;
}
```

### 7.2. Phim – item danh sách (`TMDBMovieListItem` / `TMDBMovieSummary`)

Dùng cho: discover, now_playing, upcoming, popular, search, similar, recommendations.

```typescript
interface TMDBMovieListItem {
  adult: boolean;
  backdrop_path: string | null;
  genre_ids: number[];
  id: number;
  original_language: string;
  original_title: string;
  overview: string;
  popularity: number;
  poster_path: string | null;
  release_date: string;        // "YYYY-MM-DD"
  title: string;
  video: boolean;
  vote_average: number;
  vote_count: number;
}
```

**Trending movie** thêm `media_type: "movie"` (cùng các field trên).

### 7.3. Phim – chi tiết (`GET /movie/{id}`)

```typescript
interface TMDBMovieDetailsResponse {
  adult: boolean;
  backdrop_path: string;
  belongs_to_collection: string;  // hoặc object nếu TMDB trả object – kiểm tra runtime
  budget: number;
  genres: { id: number; name: string }[];
  homepage: string;
  id: number;
  imdb_id: string;
  original_language: string;
  original_title: string;
  overview: string;
  popularity: number;
  poster_path: string;
  production_companies: {
    id: number;
    logo_path: string;
    name: string;
    origin_country: string;
  }[];
  production_countries: { iso_3166_1: string; name: string }[];
  release_date: string;
  revenue: number;
  runtime: number;                // phút
  spoken_languages: {
    english_name: string;
    iso_639_1: string;
    name: string;
  }[];
  status: string;                 // "Released", "Post Production", ...
  tagline: string;
  title: string;
  video: boolean;
  vote_average: number;
  vote_count: number;
}
```

### 7.4. Now playing / Upcoming – thêm `dates`

```typescript
interface TMDBMovieDateRange {
  maximum: string;  // "2024-12-30"
  minimum: string;  // "2024-11-01"
}

interface TMDBNowPlayingResponse extends TMDBPaginatedResponse<TMDBMovieListItem> {
  dates: TMDBMovieDateRange;
}
```

### 7.5. Credits

```typescript
interface TMDBMovieCreditsResponse {
  id: number;
  cast: {
    adult: boolean;
    gender: number;
    id: number;
    known_for_department: string;
    name: string;
    original_name: string;
    popularity: number;
    profile_path: string;
    cast_id: number;
    character: string;
    credit_id: string;
    order: number;
  }[];
  crew: {
    // ... giống cast, thêm:
    department: string;
    job: string;
  }[];
}
```

### 7.6. Videos (trailer)

```typescript
interface TMDBMovieVideosResponse {
  id: number;
  results: {
    iso_639_1: string;
    iso_3166_1: string;
    name: string;
    key: string;           // YouTube video id
    site: string;          // "YouTube"
    size: number;
    type: string;          // "Trailer", "Teaser", "Clip"
    official: boolean;
    published_at: string;
    id: string;
  }[];
}
```

**Logic chọn trailer (HomePage):**

1. YouTube + `type === "Trailer"` + `official === true`
2. YouTube + `Trailer`
3. Bất kỳ YouTube

URL embed: `https://www.youtube.com/watch?v={key}`

### 7.7. Translations (đa ngôn ngữ)

```typescript
interface TMDBMovieTranslationsResponse {
  id: number;
  translations: {
    iso_3166_1: string;
    iso_639_1: string;       // "en", "vi"
    name: string;
    english_name: string;
    data: {
      homepage: string;
      overview: string;
      runtime: number;
      tagline: string;
      title: string;
    };
  }[];
}
```

Lấy bản tiếng Việt: `translations.find(t => t.iso_639_1 === "vi")`.

### 7.8. Release dates (certification theo quốc gia)

```typescript
interface TMDBMovieReleaseDatesResponse {
  id: number;
  results: {
    iso_3166_1: string;    // "VN", "US"
    release_dates: {
      certification: string;
      descriptors: string[];
      iso_639_1: string;
      note: string;
      release_date: string;
      type: number;
    }[];
  }[];
}
```

### 7.9. Watch providers

```typescript
interface TMDBMovieWatchProvidersResponse {
  id: number;
  results: Record<string, {
    link: string;
    flatrate?: { provider_id: number; provider_name: string; logo_path: string }[];
    rent?: {};
    buy?: {}
  }>;
}
```

Ví dụ Việt Nam: `results["VN"]`.

### 7.10. Reviews

```typescript
interface TMDBMovieReviewsResponse extends TMDBPaginatedResponse<{
  author: string;
  author_details: { name: string; username: string; avatar_path: string; rating: string };
  content: string;
  created_at: string;
  id: string;
  updated_at: string;
  url: string;
}> {
  id: number;
}
```

### 7.11. Search movie

Giống `TMDBMovieListItem` (xem `client/src/module/search/database/interface/search.ts`).

### 7.12. Genre

```typescript
interface TMDBGenreListResponse {
  genres: { id: number; name: string }[];
}
```

Gọi: `GET /genre/movie/list?language=vi-VN`

### 7.13. Collection (franchise phim lẻ)

```typescript
interface TMDBCollectionDetailsResponse {
  id: number;
  name: string;
  overview: string;
  poster_path: string | null;
  backdrop_path: string | null;
  parts: {
    id: number;
    title: string;
    release_date: string;
    poster_path: string | null;
    media_type: string;
    // ...
  }[];
}
```

---

## 8. Chi tiết phim – `append_to_response`

MOIVEZONE gom **1 request** thay vì 10+ request (`DetailsPage.tsx`).

```
GET /movie/{id}?language=vi-VN&append_to_response=account_states,alternative_titles,changes,credits,external_ids,images,keywords,lists,reviews,translations,videos,watch/providers,similar,recommendations,release_dates
```

**Lưu ý:** `watch/providers` có dấu `/` – giữ nguyên trong query.

Response là object **merge**: field gốc của movie + các key trùng tên sub-API.

Ví dụ cấu trúc (rút gọn):

```json
{
  "id": 550,
  "title": "...",
  "overview": "...",
  "credits": { "cast": [], "crew": [] },
  "videos": { "results": [] },
  "similar": { "page": 1, "results": [] },
  "recommendations": { "page": 1, "results": [] },
  "translations": { "translations": [] },
  "release_dates": { "results": [] },
  "watch/providers": { "results": { "VN": {} } }
}
```

**Dự án Cinema (trang `customer/movie-detail`):** backend gọi `GET /movie/{id}` với `append_to_response=videos,credits,images,similar,recommendations` (rút gọn so với ví dụ đầy đủ ở trên). `TmdbCatalogService#mapDetail` map sang DTO: `credits.cast` (tối đa 80, kèm `person_id`), `videos.results` (YouTube, `name`/`published_at`), `images.backdrops|posters|logos` (URL qua `TmdbImageUrlBuilder`), `similar.results` / `recommendations.results` (carousel phim).

**Spring Boot:** một DTO `MovieDetailsAggregate` với `@JsonProperty("watch/providers")` hoặc `@JsonAlias`.

---

## 9. Tích hợp Spring Boot

### 9.1. Cấu trúc package gợi ý

```
com.yourapp.tmdb
  config/TmdbProperties.java
  config/TmdbWebClientConfig.java
  client/TmdbClient.java
  dto/...          // mirror các interface trên
  service/MovieService.java
  mapper/MovieMapper.java
  controller/MovieController.java
```

### 9.2. Properties

```java
@ConfigurationProperties(prefix = "tmdb")
public record TmdbProperties(
    String baseUrl,
    String apiKey,
    String bearerToken,
    int timeoutMs
) {}
```

### 9.3. WebClient – gọi API có `language`

```java
@Service
@RequiredArgsConstructor
public class TmdbClient {

    private final WebClient tmdbWebClient;
    private final TmdbProperties props;

    public Mono<MovieListPageDto> discoverLatestMovies(int page, String language) {
        return tmdbWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/discover/movie")
                .queryParam("api_key", props.apiKey())
                .queryParam("page", page)
                .queryParam("language", language)
                .queryParam("sort_by", "primary_release_date.desc")
                .queryParam("include_adult", false)
                .queryParam("include_video", false)
                .build())
            .retrieve()
            .bodyToMono(MovieListPageDto.class);
    }

    public Mono<MovieDetailsDto> getMovieDetails(long id, String language) {
        String append = String.join(",",
            "credits", "videos", "images", "keywords", "reviews",
            "translations", "similar", "recommendations", "release_dates",
            "watch/providers", "external_ids");

        return tmdbWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/movie/{id}")
                .queryParam("api_key", props.apiKey())
                .queryParam("language", language)
                .queryParam("append_to_response", append)
                .build(id))
            .retrieve()
            .bodyToMono(MovieDetailsDto.class);
    }
}
```

### 9.4. DTO Java – ví dụ item danh sách

```java
@Data
public class MovieSummaryDto {
    private Long id;
    private String title;
    private String originalTitle;
    @JsonProperty("original_title")
    public void setOriginalTitle(String v) { this.originalTitle = v; }
    private String overview;
    @JsonProperty("poster_path")
    private String posterPath;
    @JsonProperty("backdrop_path")
    private String backdropPath;
    @JsonProperty("release_date")
    private String releaseDate;
    @JsonProperty("vote_average")
    private Double voteAverage;
    @JsonProperty("vote_count")
    private Integer voteCount;
    private Double popularity;
    @JsonProperty("genre_ids")
    private List<Integer> genreIds;
}
```

```java
@Data
public class MovieListPageDto {
    private int page;
    private List<MovieSummaryDto> results;
    @JsonProperty("total_pages")
    private int totalPages;
    @JsonProperty("total_results")
    private int totalResults;
}
```

### 9.5. Service – chỉ phim mới + đa ngôn ngữ

```java
@Service
@RequiredArgsConstructor
public class MovieService {

    private final TmdbClient tmdb;
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/";

    public MovieListPageDto listLatest(String lang, int page) {
        return tmdb.discoverLatestMovies(page, lang).block();
    }

    public MovieListPageDto listNowPlaying(String lang, int page) {
        // GET /movie/now_playing
    }

    public MovieListPageDto listUpcoming(String lang, int page) {
        // GET /movie/upcoming
    }

    public String posterUrl(String path, String size) {
        if (path == null || path.isBlank()) return null;
        return IMAGE_BASE + size + path;
    }
}
```

### 9.6. REST Controller mẫu

```java
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/latest")
    public MovieListPageDto latest(
        @RequestParam(defaultValue = "vi-VN") String lang,
        @RequestParam(defaultValue = "1") int page
    ) {
        return movieService.listLatest(lang, page);
    }

    @GetMapping("/{id}")
    public MovieDetailsDto detail(
        @PathVariable long id,
        @RequestParam(defaultValue = "vi-VN") String lang
    ) {
        return movieService.getDetails(id, lang);
    }
}
```

### 9.7. Cache & rate limit

- Cache: `configuration`, `genre/movie/list` (theo `language`), TTL 6–24h.
- Không cache vô hạn `trending` / `now_playing` (đổi theo ngày).
- TMDB giới hạn ~40 request / 10 giây / IP – dùng cache phía server.

### 9.8. Xử lý lỗi

| HTTP | Ý nghĩa |
|------|---------|
| 401 | Sai `api_key` / token |
| 404 | Sai `movie_id` |
| 429 | Rate limit – backoff |

`useFetchDetails` map lỗi: `{status} - {statusText}`.

---

## 10. Bảng tham chiếu nhanh

### 10.1. Endpoint nên dùng (phim lẻ + mới)

| Use case | Endpoint | language |
|----------|----------|----------|
| Danh sách phim mới nhất | `GET /discover/movie?sort_by=primary_release_date.desc` | vi-VN / en-US |
| Phim năm hiện tại | `+ primary_release_year={year}` | |
| Đang chiếu | `GET /movie/now_playing` | |
| Sắp chiếu | `GET /movie/upcoming` | |
| Xu hướng | `GET /trending/movie/day` | |
| Chi tiết đầy đủ | `GET /movie/{id}?append_to_response=...` | |
| Tìm kiếm | `GET /search/movie?query=` | |
| Thể loại | `GET /genre/movie/list` | |
| Trailer | `GET /movie/{id}/videos` | |
| Diễn viên | `GET /person/{id}/movie_credits` | |

### 10.2. Không dùng (theo yêu cầu)

- Mọi `/tv/*`, `/discover/tv`, `/trending/tv`, `/search/tv`
- `GET /movie/top_rated` (phim cũ)
- `GET /movie/popular` nếu mục tiêu chỉ “mới”

### 10.3. Map file TypeScript → Java DTO

| TypeScript | Java DTO gợi ý |
|------------|----------------|
| `TMDBMovieListItem` | `MovieSummaryDto` |
| `TMDBMovieDetailsResponse` | `MovieDetailsDto` |
| `TMDBPaginatedResponse<T>` | `PageDto<T>` |
| `TMDBMovieCreditsResponse` | `MovieCreditsDto` |
| `TMDBMovieVideosResponse` | `MovieVideosDto` |
| `TMDBMovieTranslationsResponse` | `MovieTranslationsDto` |
| `AppLanguage` | `enum AppLanguage` |

### 10.4. Tham số `language` chuẩn app

| UI | Query `language` |
|----|------------------|
| Tiếng Việt | `vi-VN` |
| English | `en-US` |

---

## Phụ lục A – Ví dụ response rút gọn

### Discover movie (page 1)

```json
{
  "page": 1,
  "total_pages": 500,
  "total_results": 10000,
  "results": [
    {
      "id": 123,
      "title": "Tên phim",
      "original_title": "Original Title",
      "overview": "...",
      "poster_path": "/abc.jpg",
      "backdrop_path": "/def.jpg",
      "release_date": "2025-05-18",
      "vote_average": 7.2,
      "vote_count": 150,
      "popularity": 120.5,
      "genre_ids": [28, 12],
      "adult": false,
      "video": false,
      "original_language": "en"
    }
  ]
}
```

### Movie detail (không append)

```json
{
  "id": 123,
  "title": "Tên phim",
  "overview": "...",
  "runtime": 120,
  "release_date": "2025-05-18",
  "genres": [{ "id": 28, "name": "Hành động" }],
  "poster_path": "/abc.jpg",
  "backdrop_path": "/def.jpg",
  "vote_average": 7.2,
  "vote_count": 150,
  "status": "Released",
  "tagline": "...",
  "imdb_id": "tt1234567"
}
```

---

## Phụ lục B – Checklist tích hợp Spring Boot

- [ ] Đăng ký TMDB, lấy API Key v3 (và token v4 nếu cần)
- [ ] Cấu hình `tmdb.api-key` qua biến môi trường
- [ ] WebClient/RestTemplate + timeout 8s
- [ ] Enum `vi-VN` / `en-US` trên mọi API public của bạn
- [ ] Service discover với `primary_release_date.desc`
- [ ] (Tùy chọn) `primary_release_year` hoặc `primary_release_date.gte`
- [ ] Helper `buildImageUrl(size, path)`
- [ ] Detail dùng `append_to_response`
- [ ] Cache configuration + genres
- [ ] Không gọi endpoint TV

---

*Tài liệu sinh từ codebase MOIVEZONE. Cập nhật khi đổi logic filter phim mới trong `AllMoviesPage.tsx` / `TMDBDiscover.api.ts`.*
