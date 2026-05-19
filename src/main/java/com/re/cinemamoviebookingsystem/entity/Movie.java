package com.re.cinemamoviebookingsystem.entity;

import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "tmdb_id", nullable = false, unique = true)
    private Long tmdbId;

    /** Thời lượng chiếu (phút) — dùng tính end_time suất chiếu. */
    @Column(nullable = false)
    private Integer duration;

    @Column(name = "age_label", length = 16)
    private String ageLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MovieStatus status = MovieStatus.ACTIVE;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "unpublished_at")
    private LocalDateTime unpublishedAt;

    @Column(name = "default_base_price", precision = 12, scale = 2)
    private BigDecimal defaultBasePrice;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "runtime_synced_at")
    private LocalDateTime runtimeSyncedAt;
}
