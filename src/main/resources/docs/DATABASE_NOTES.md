# Ghi chú cơ sở dữ liệu

## Ba file SQL trong `src/main/resources/db/`

| File | Vai trò |
|------|---------|
| **`schema.sql`** | Schema đầy đủ để import thủ công (Workbench). **Không** tự chạy khi `spring bootRun`. |
| **`seed.sql`** | Dữ liệu mẫu: roles, users, rooms, seats, combos. **Có** chạy mỗi lần khởi động (`spring.sql.init.mode=always`). |
| **`schema-migration-legacy.sql`** | Script tùy chọn dọn DB cũ (xóa `genres`, truncate phim/suat cũ). Chạy tay, có backup. |

Khi dev bình thường: **Hibernate** (`ddl-auto=update`) đồng bộ bảng từ **entity**; `schema.sql` chỉ để bạn đọc / tạo DB mới từ đầu.

## Bảng `movies` (mô hình mới)

Chỉ lưu thông tin **rạp**, không lưu title/poster/overview (lấy từ TMDB theo `tmdb_id`):

- `tmdb_id` (bắt buộc, unique)
- `duration`, `age_label`, `status`
- `published_at`, `unpublished_at`, `default_base_price`, `admin_note`, `runtime_synced_at`

Đã **bỏ**: `genres`, `movie_genres`, và các cột metadata cũ trên `movies`.

## Luồng nghiệp vụ

1. Admin **Đăng phim từ TMDB** → `CinemaMovieService.publishToCinema()` → insert `movies` + `ShowtimeScheduleService` tạo lịch mẫu.
2. Khách trang chủ / lịch: chỉ phim **ACTIVE** có suất sắp tới.
3. Khách xem chi tiết TMDB: có thể xem metadata; đặt vé chỉ khi phim đã đăng tại rạp.

## DB cũ trên máy bạn

Nếu vẫn thấy bảng `genres` hoặc cột `title` trên `movies`: đó là dữ liệu/schema cũ. Hibernate `update` **thêm** cột mới, **không** xóa bảng/cột cũ. Dùng `schema-migration-legacy.sql` hoặc tạo database mới rồi chạy `schema.sql` + khởi động app.

## File không còn dùng

- `tmdb-backfill.sql` — map phim seed cũ sang TMDB; đã gỡ khỏi `application.properties`.
