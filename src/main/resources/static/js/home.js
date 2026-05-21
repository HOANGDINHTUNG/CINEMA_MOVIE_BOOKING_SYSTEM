(function () {
    const body = document.body;
    if (!body || !body.dataset.appLang) {
        return;
    }

    initHomePromoModal();
    initHomeSidebarCarousels();

    const lang = body.dataset.appLang;
    const labels = {
        nextShowtime: body.dataset.labelNextShowtime || 'Suất gần nhất',
        inCinema: body.dataset.labelInCinema || 'Đang chiếu',
        releaseDate: body.dataset.labelReleaseDate || 'Khởi chiếu'
    };

    function posterUrl(url) {
        return url && url.startsWith('http') ? url : '/assets/img/poster.jpg';
    }

    function escapeHtml(text) {
        if (!text) return '';
        const el = document.createElement('div');
        el.textContent = text;
        return el.innerHTML;
    }

    function buildCardHtml(movie, showSchedule) {
        let html = '<div class="film-card"><a href="/customer/movies/' + movie.tmdbId + '">';
        html += '<div class="image-wrapper"><img src="' + escapeHtml(posterUrl(movie.posterUrl)) + '" alt="'
            + escapeHtml(movie.title) + '" loading="lazy"/></div>';
        html += '<div class="film-title">' + escapeHtml(movie.title) + '</div>';
        if (movie.genresLabel) {
            html += '<div class="description">' + escapeHtml(movie.genresLabel) + '</div>';
        }
        if (showSchedule && movie.nextShowtime) {
            const dt = new Date(movie.nextShowtime);
            const pad = (v) => String(v).padStart(2, '0');
            html += '<div class="description">' + labels.nextShowtime + ': ' + pad(dt.getDate()) + '/'
                + pad(dt.getMonth() + 1) + ' ' + pad(dt.getHours()) + ':' + pad(dt.getMinutes()) + '</div>';
        }
        if (!showSchedule && movie.releaseDateLabel) {
            html += '<div class="description">' + labels.releaseDate + ': ' + escapeHtml(movie.releaseDateLabel) + '</div>';
        }
        if (!showSchedule && movie.voteAverage != null) {
            html += '<div class="description">★ ' + Number(movie.voteAverage).toFixed(1) + '</div>';
        }
        if (showSchedule && movie.hasShowtimes) {
            html += '<span class="badge bg-success mt-1">' + escapeHtml(labels.inCinema) + '</span>';
        }
        html += '</a></div>';
        return html;
    }

    function setupLoadMore(btnId, wrapId, gridId, section, startPage) {
        const btn = document.getElementById(btnId);
        const wrap = document.getElementById(wrapId);
        const grid = document.getElementById(gridId);
        if (!btn || !wrap || !grid) return;

        const pager = { page: startPage };
        btn.addEventListener('click', async function () {
            btn.disabled = true;
            const old = btn.textContent;
            btn.textContent = btn.dataset.loadingLabel || '...';
            try {
                const res = await fetch('/api/public/home/' + section + '?lang=' + encodeURIComponent(lang) + '&page=' + pager.page);
                if (!res.ok) throw new Error('HTTP ' + res.status);
                const data = await res.json();
                const movies = data.movies || [];
                const showSchedule = section === 'now-showing';
                movies.forEach(function (m) {
                    grid.insertAdjacentHTML('beforeend', buildCardHtml(m, showSchedule));
                });
                pager.page = (data.page || pager.page) + 1;
                if (!data.hasMore || movies.length === 0) wrap.classList.add('d-none');
            } catch (e) {
                console.error(e);
            } finally {
                btn.disabled = false;
                btn.textContent = old;
            }
        });
    }

    setupLoadMore('loadMoreNowShowing', 'nowShowingLoadMoreWrap', 'nowShowingGrid', 'now-showing', 2);
    setupLoadMore('loadMoreComingSoon', 'comingSoonLoadMoreWrap', 'comingSoonGrid', 'coming-soon', 2);
    initTrendingPeriodToggle(lang);
})();

function initTrendingPeriodToggle(lang) {
    const toggle = document.getElementById('homeTrendingPeriodToggle');
    const list = document.getElementById('trendingList');
    if (!toggle || !list) {
        return;
    }

    function buildTrendingItemHtml(movie) {
        const title = movie.title || '';
        const url = '/customer/movies/' + movie.tmdbId;
        const poster = movie.posterUrl && movie.posterUrl.startsWith('http')
            ? movie.posterUrl : '/assets/img/poster.jpg';
        const rating = movie.voteAverage != null
            ? '<span class="trending-item-rating">★ ' + Number(movie.voteAverage).toFixed(1) + '</span>' : '';
        return '<div class="trending-item">'
            + '<a href="' + url + '" class="trending-item-link">'
            + '<div class="trending-item-poster"><img src="' + poster + '" alt="" loading="lazy"/></div>'
            + '<div class="trending-item-meta"><span class="trending-item-title">'
            + escapeHtmlTrending(title) + '</span>' + rating + '</div></a></div>';
    }

    function escapeHtmlTrending(text) {
        if (!text) return '';
        const el = document.createElement('div');
        el.textContent = text;
        return el.innerHTML;
    }

    toggle.querySelectorAll('[data-trending-window]').forEach(function (btn) {
        btn.addEventListener('click', async function () {
            const windowKey = btn.getAttribute('data-trending-window');
            if (!windowKey || btn.classList.contains('is-active')) {
                return;
            }
            toggle.querySelectorAll('.home-period-toggle__btn').forEach(function (b) {
                b.classList.toggle('is-active', b === btn);
            });
            list.classList.add('is-loading');
            try {
                const res = await fetch('/api/public/home/trending?lang='
                    + encodeURIComponent(lang) + '&window=' + encodeURIComponent(windowKey));
                if (!res.ok) {
                    throw new Error('HTTP ' + res.status);
                }
                const movies = await res.json();
                list.innerHTML = '';
                (movies || []).forEach(function (m) {
                    if (m && m.tmdbId) {
                        list.insertAdjacentHTML('beforeend', buildTrendingItemHtml(m));
                    }
                });
                document.body.dataset.trendingWindow = windowKey;
            } catch (e) {
                console.error(e);
            } finally {
                list.classList.remove('is-loading');
            }
        });
    });
}

function initHomeSidebarCarousels() {
    const AUTO_MS = 4500;
    const roots = document.querySelectorAll('.home-sidebar-carousel__root');
    if (!roots.length) {
        return;
    }

    roots.forEach(function (root) {
        const slideCount = parseInt(root.getAttribute('data-slide-count') || '0', 10);
        if (slideCount < 2) {
            return;
        }

        const slides = root.querySelectorAll('.home-sidebar-carousel__slide');
        const dots = root.querySelectorAll('.home-sidebar-carousel__dot');
        if (!slides.length) {
            return;
        }

        let index = 0;
        let timer = null;
        const section = root.closest('.home-sidebar-carousel');

        function showSlide(nextIndex) {
            index = (nextIndex + slides.length) % slides.length;
            slides.forEach(function (slide, i) {
                slide.classList.toggle('is-active', i === index);
            });
            dots.forEach(function (dot, i) {
                dot.classList.toggle('is-active', i === index);
                dot.setAttribute('aria-selected', i === index ? 'true' : 'false');
            });
        }

        function nextSlide() {
            showSlide(index + 1);
        }

        function startAutoplay() {
            stopAutoplay();
            timer = window.setInterval(nextSlide, AUTO_MS);
            if (section) {
                section.classList.remove('is-paused');
            }
        }

        function stopAutoplay() {
            if (timer) {
                window.clearInterval(timer);
                timer = null;
            }
            if (section) {
                section.classList.add('is-paused');
            }
        }

        dots.forEach(function (dot) {
            dot.addEventListener('click', function () {
                const to = parseInt(dot.getAttribute('data-slide-to') || '0', 10);
                showSlide(to);
                startAutoplay();
            });
        });

        root.addEventListener('mouseenter', stopAutoplay);
        root.addEventListener('mouseleave', startAutoplay);
        root.addEventListener('focusin', stopAutoplay);
        root.addEventListener('focusout', startAutoplay);

        startAutoplay();
    });
}

function initHomePromoModal() {
    const overlay = document.getElementById('homePromoModal');
    if (!overlay) return;

    const closeBtn = document.getElementById('homePromoCloseBtn');

    function openPromo() {
        overlay.hidden = false;
        document.body.classList.add('home-promo-open');
    }

    function closePromo() {
        overlay.hidden = true;
        document.body.classList.remove('home-promo-open');
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', function (event) {
            event.preventDefault();
            closePromo();
        });
    }

    overlay.addEventListener('click', function (event) {
        if (event.target === overlay) {
            closePromo();
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !overlay.hidden) {
            closePromo();
        }
    });

    /* Hiện popup mỗi lần vào trang chủ (sau loader) */
    function scheduleOpen() {
        setTimeout(openPromo, 320);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', scheduleOpen);
    } else {
        scheduleOpen();
    }
}
