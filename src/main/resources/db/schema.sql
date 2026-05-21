-- =============================================================================
-- Smart Cinema — schema tham chieu (import thu cong qua MySQL Workbench neu can)
--
-- Khi chay Spring Boot (mặc dinh):
--   spring.jpa.hibernate.ddl-auto=update  → Hibernate tao/sua bang tu entity
--   spring.sql.init.data-locations=classpath:db/seed.sql  → nap du lieu mau
--
-- File nay KHONG tu chay khi khoi dong app (khong khai bao trong application.properties).
-- Can dong bo voi entity Movie.java (TMDB-first: chi lu du lieu rạp, khong lu title/poster).
-- =============================================================================
CREATE DATABASE IF NOT EXISTS smart_cinema_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_cinema_db;

CREATE TABLE roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role_id INT NOT NULL,
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE user_profiles (
    profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(15) NULL,
    avatar_url VARCHAR(512) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Phim dang chieu tai rap: metadata hien thi lay tu TMDB theo tmdb_id
CREATE TABLE movies (
    movie_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tmdb_id BIGINT NOT NULL,
    duration INT NOT NULL COMMENT 'Thoi luong phut (tinh end_time suat)',
    age_label VARCHAR(16) NULL COMMENT 'T13, K, ...',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    published_at DATETIME NULL,
    unpublished_at DATETIME NULL,
    default_base_price DECIMAL(12,2) NULL COMMENT 'Gia mac dinh khi admin tao suat',
    admin_note TEXT NULL,
    runtime_synced_at DATETIME NULL COMMENT 'Lan cuoi cap nhat runtime/age tu TMDB',
    CONSTRAINT uq_movies_tmdb_id UNIQUE (tmdb_id),
    CONSTRAINT chk_movie_duration CHECK (duration > 0),
    CONSTRAINT chk_movie_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_movies_status_published ON movies (status, published_at);

CREATE TABLE rooms (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(50) NOT NULL UNIQUE,
    total_seats INT NOT NULL,
    CONSTRAINT chk_room_seats CHECK (total_seats > 0)
);

CREATE TABLE seats (
    seat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    row_name VARCHAR(5) NOT NULL,
    seat_number INT NOT NULL,
    seat_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    CONSTRAINT chk_seat_type CHECK (seat_type IN ('STANDARD', 'VIP')),
    CONSTRAINT uq_room_seat_position UNIQUE (room_id, row_name, seat_number)
);

CREATE TABLE showtimes (
    showtime_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    room_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE RESTRICT,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE RESTRICT,
    CONSTRAINT chk_showtime_dates CHECK (end_time > start_time),
    CONSTRAINT chk_showtime_price CHECK (base_price >= 0),
    CONSTRAINT chk_showtime_status CHECK (status IN ('ACTIVE', 'SOLD_OUT', 'HIDDEN', 'CANCELLED'))
);

CREATE TABLE showtime_seats (
    showtime_seat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    showtime_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    locked_by_user BIGINT NULL,
    locked_until DATETIME NULL,
    FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE CASCADE,
    FOREIGN KEY (locked_by_user) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT chk_showtime_seat_status CHECK (status IN ('AVAILABLE', 'LOCKED', 'BOOKED')),
    CONSTRAINT uq_showtime_seat UNIQUE (showtime_id, seat_id)
);

CREATE TABLE combos (
    combo_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_combo_price CHECK (price >= 0),
    CONSTRAINT chk_combo_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE vouchers (
    voucher_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    description TEXT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    min_order_amount DECIMAL(12,2) NULL DEFAULT 0,
    max_discount_amount DECIMAL(12,2) NULL,
    valid_from DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    require_combo TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT chk_voucher_discount_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT chk_voucher_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE user_vouchers (
    user_voucher_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    claimed_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    used_booking_id BIGINT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id) ON DELETE CASCADE,
    CONSTRAINT chk_user_voucher_status CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED'))
);

CREATE INDEX idx_user_vouchers_user_status ON user_vouchers (user_id, status);
CREATE INDEX idx_vouchers_valid ON vouchers (status, valid_until);

CREATE TABLE bookings (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    showtime_id BIGINT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    subtotal_amount DECIMAL(10,2) NULL,
    discount_amount DECIMAL(10,2) NULL DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL,
    user_voucher_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id) ON DELETE RESTRICT,
    CONSTRAINT chk_booking_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_booking_status CHECK (status IN ('HELD', 'PENDING', 'PAID', 'CANCELLED'))
);

CREATE TABLE tickets (
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    ticket_code VARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE RESTRICT,
    CONSTRAINT chk_ticket_price CHECK (price >= 0)
);

CREATE TABLE booking_combos (
    booking_id BIGINT NOT NULL,
    combo_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (booking_id, combo_id),
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    FOREIGN KEY (combo_id) REFERENCES combos(combo_id) ON DELETE RESTRICT,
    CONSTRAINT chk_combo_quantity CHECK (quantity > 0),
    CONSTRAINT chk_booking_combo_price CHECK (price >= 0)
);

CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100) NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_date DATETIME NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

CREATE TABLE content_articles (
    id VARCHAR(64) PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(500) NOT NULL,
    published_date DATE NULL,
    thumbnail VARCHAR(255) NULL,
    images TEXT NULL,
    content_html MEDIUMTEXT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT chk_content_category CHECK (category IN ('PROMOTION', 'NEWS', 'FESTIVAL'))
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(50) NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(50) NULL,
    entity_id VARCHAR(50) NULL,
    detail TEXT NULL,
    created_at DATETIME NULL
);


CREATE INDEX idx_content_articles_category ON content_articles (category);
CREATE INDEX idx_showtimes_conflict ON showtimes (room_id, start_time, end_time);
CREATE INDEX idx_showtimes_movie_start ON showtimes (movie_id, start_time);
CREATE INDEX idx_showtime_seats_ttl ON showtime_seats (status, locked_until);
CREATE INDEX idx_bookings_dashboard ON bookings (status, booking_date);
CREATE INDEX idx_tickets_booking ON tickets (booking_id);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at);
