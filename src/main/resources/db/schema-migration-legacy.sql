-- =============================================================================
-- Migration thu cong: DB cu (title, genres, movie_genres) → mo hinh TMDB-first
-- CHI chay khi ban da co database cu va muon don schema.
-- Backup truoc khi chay. Khong chay tu dong khi khoi dong Spring Boot.
-- =============================================================================
USE smart_cinema_db;

-- 1) Xoa bang lien ket the loai (khong con dung)
DROP TABLE IF EXISTS movie_genres;
DROP TABLE IF EXISTS genres;

-- 2) Don du lieu phim/suat cu (neu phim seed khong co tmdb_id hop le)
--    Sau do admin dang lai phim qua /admin/movies/import
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE showtime_seats;
TRUNCATE TABLE tickets;
TRUNCATE TABLE booking_combos;
TRUNCATE TABLE payments;
TRUNCATE TABLE bookings;
TRUNCATE TABLE showtimes;
TRUNCATE TABLE movies;
SET FOREIGN_KEY_CHECKS = 1;

-- 3) Cot moi (bo qua loi "Duplicate column" neu Hibernate da them khi chay app)
-- ALTER TABLE movies ADD COLUMN tmdb_id BIGINT NULL;
-- ALTER TABLE movies ADD COLUMN age_label VARCHAR(16) NULL;
-- ALTER TABLE movies ADD COLUMN published_at DATETIME NULL;
-- ALTER TABLE movies ADD COLUMN unpublished_at DATETIME NULL;
-- ALTER TABLE movies ADD COLUMN default_base_price DECIMAL(12,2) NULL;
-- ALTER TABLE movies ADD COLUMN admin_note TEXT NULL;
-- ALTER TABLE movies ADD COLUMN runtime_synced_at DATETIME NULL;

-- 4) Bo cot metadata cu (chi khi chac khong can du lieu cu)
-- ALTER TABLE movies
--     DROP COLUMN title,
--     DROP COLUMN description,
--     DROP COLUMN release_date,
--     DROP COLUMN poster_url,
--     DROP COLUMN trailer_url,
--     DROP COLUMN tmdb_synced_at;

-- 5) Bat buoc tmdb_id cho phim moi (sau khi da co it nhat 1 phim hop le hoac bang trong)
-- ALTER TABLE movies MODIFY tmdb_id BIGINT NOT NULL;
-- ALTER TABLE movies ADD CONSTRAINT uq_movies_tmdb_id UNIQUE (tmdb_id);
