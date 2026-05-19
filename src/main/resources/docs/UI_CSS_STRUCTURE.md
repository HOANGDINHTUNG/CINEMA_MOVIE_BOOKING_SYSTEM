# Cấu trúc CSS / HTML khách hàng

## Trang chủ & catalog

| File | Mục đích |
|------|----------|
| `homepage.css` | Import 3 module (không thêm rule trực tiếp) |
| `home-layout.css` | Font, lưới phim `.film-card`, sidebar, top-bar |
| `hero-banner.css` | Carousel banner TMDB |
| `home-responsive.css` | Media queries (đã bỏ Swiper / mockup cũ) |

Templates: `customer/home.html`, `fragments/customer/hero-slide.html`, `fragments/customer/home-movie-card.html`

## Trang khác

Mỗi màn giữ CSS riêng: `movie-details.css`, `calendar.css`, `payment.css`, `payment-success.css`, …

## Đã xóa

- `templates/_archive/static-mockups/` — prototype HTML tĩnh
- `static/css/style.css` — không được link
- `fragments/customer/movie-card.html` — không dùng (thay bằng `home-movie-card.html`)
