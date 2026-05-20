# Ghi chú cơ sở dữ liệu

## Hai file SQL chính trong `src/main/resources/db/`

**Quy ước dự án:** mọi thay đổi schema / dữ liệu mẫu **chỉ** ghi vào hai file sau — **không** tạo thêm `migration-*.sql`, `seed-*-generated.sql`, v.v.

| File | Vai trò |
|------|---------|
| **`schema.sql`** | Schema đầy đủ (bảng, constraint, index). Import thủ công hoặc tham chiếu khi tạo DB mới. |
| **`seed.sql`** | Toàn bộ dữ liệu mẫu (`INSERT IGNORE` …). **Tự chạy** mỗi lần khởi động (`spring.sql.init.data-locations=classpath:db/seed.sql`). |

Khi dev bình thường: **Hibernate** (`ddl-auto=update`) đồng bộ bảng từ **entity**; `schema.sql` là nguồn chân lý khi cần đọc / tạo DB sạch.

**Trạng thái `HELD` (Đang thanh toán):** `schema.sql` đã có `HELD` trong `chk_booking_status`. DB cũ: app tự sửa constraint qua `BookingStatusConstraintMigration` (Java), không cần file SQL riêng.

Script Python (`generate_*_seed.py`) nếu dùng thì **ghi kết quả vào `seed.sql`**, không xuất file SQL phụ.

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

## Nội dung web (khuyến mãi / tin / lễ hội)

- Bảng **`content_articles`** (JPA entity `ContentArticle`).
- Dữ liệu mẫu nằm trong **`db/seed.sql`** (sinh từ `database/promotion.json`, `event.json`, `festival.json` qua `scripts/generate_content_seed.py`).
- Nếu bảng trống và chưa có trong seed: `ContentJsonImportRunner` vẫn import JSON classpath dự phòng.
- Admin và trang khách đọc/ghi qua DB.

## Catalog TMDB demo (100 + 100 phim)

- File `DemoTmdbCatalog.java`: 100 id **đang hot** (trending + now playing + discover) và 100 id **sắp chiếu** (upcoming + discover).
- Sinh lại danh sách: `python scripts/fetch_tmdb_demo_catalog.py` (cần `tmdb.api-key`).
- Khi khởi động: `CinemaDemoSeedRunner` đăng phim thiếu tới đủ `cinema.demo-seed-now-showing-target` / `coming-soon-target` (mặc định 100).

## Phim / user bổ sung từ JSON

- `database/movies.json` → `seed.sql` (cột `tmdb_id`, `duration`, …) qua `scripts/generate_movies_seed.py`.
- `database/user.json` → user id ≥ 100 trong `seed.sql` qua `scripts/generate_users_seed.py`.
- Chạy lại toàn bộ: `python scripts/generate_content_seed.py` (+ movies/users) rồi `python scripts/merge_seed_sql.py`.

## File JSON legacy (không đọc runtime)

- `movies.json`, `rooms.json`, `seats.json`, `user.json`, `db.json` — có thể xóa.

## File không còn dùng

- `tmdb-backfill.sql` — map phim seed cũ sang TMDB; đã gỡ khỏi `application.properties`.
