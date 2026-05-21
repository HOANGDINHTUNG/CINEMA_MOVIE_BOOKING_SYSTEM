(function () {
    'use strict';

    const body = document.body;
    if (body.dataset.asyncHub !== 'true') {
        return;
    }

    const lang = body.dataset.lang || 'vi-VN';
    const keyword = body.dataset.keyword || '';
    const csrfParam = body.dataset.csrfParam || '';
    const csrfToken = body.dataset.csrfToken || '';
    const hubPhase = body.dataset.hubPhase || '';

    const PHASE_META = {
        WAITING_SCHEDULE: { css: 'phase-waiting', icon: 'fa-hourglass-half', label: 'Đang đợi lịch chiếu' },
        HAS_SCHEDULE: { css: 'phase-scheduled', icon: 'fa-calendar-check', label: 'Đã có lịch chiếu' },
        ENDED: { css: 'phase-ended', icon: 'fa-flag-checkered', label: 'Hết chiếu tại rạp' }
    };

    function escapeHtml(text) {
        if (text == null) return '';
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    function skeletonCards(n) {
        let html = '';
        for (let i = 0; i < n; i++) {
            html += '<article class="movie-card bg-white rounded-lg shadow overflow-hidden movie-card--skeleton">'
                + '<div class="movie-card__poster-placeholder animate-pulse"></div>'
                + '<div class="p-3"><div class="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>'
                + '<div class="h-3 bg-gray-100 rounded w-1/2"></div></div></article>';
        }
        return html;
    }

    function formatMeta(m) {
        const parts = ['#' + m.movieId];
        if (m.tmdbId) parts.push('TMDB ' + m.tmdbId);
        if (m.duration) parts.push(m.duration + 'p');
        return parts.join(' · ');
    }

    function phaseMeta(phase) {
        return PHASE_META[phase] || { css: '', icon: 'fa-film', label: phase || '' };
    }

    function buildCard(m) {
        const pm = phaseMeta(m.phase);
        const poster = m.posterUrl
            ? '<img src="' + escapeHtml(m.posterUrl) + '" alt="" class="movie-card__poster" loading="lazy"/>'
            : '<div class="movie-card__poster-placeholder"><i class="fas fa-film"></i></div>';
        const scheduleBtn = m.phase === 'WAITING_SCHEDULE'
            ? '<form action="/admin/movies/' + m.movieId + '/regenerate-schedule" method="post" class="inline">'
            + '<input type="hidden" name="' + escapeHtml(csrfParam) + '" value="' + escapeHtml(csrfToken) + '"/>'
            + '<button type="submit" class="text-amber-700 hover:underline" onclick="return confirm(\'Tạo lịch mẫu 10 ngày?\')">Tạo lịch mẫu</button></form>'
            : '';
        const canHide = m.status === 'ACTIVE' && m.canDeactivate !== false
            && (!m.audienceBookings || m.audienceBookings === 0);
        const hideBtn = canHide
            ? '<form action="/admin/movies/' + m.movieId + '/delete" method="post" class="inline">'
            + '<input type="hidden" name="' + escapeHtml(csrfParam) + '" value="' + escapeHtml(csrfToken) + '"/>'
            + '<button type="submit" class="text-red-600 hover:underline" onclick="return confirm(\'Ẩn phim khỏi rạp? Phim chưa có khách đặt vé.\')">Ẩn</button></form>'
            : (m.status === 'ACTIVE'
                ? '<span class="text-gray-400 cursor-not-allowed" title="Phim đã có người đặt vé">Ẩn</span>'
                : '');
        const bookingLine = (m.audienceBookings && m.audienceBookings > 0)
            ? '<p class="text-xs text-amber-800 mt-2 font-medium"><i class="fas fa-users"></i> Đã có '
            + escapeHtml(m.audienceBookings) + ' đơn đặt vé — không thể ẩn phim</p>'
            : '';
        let statusLine = '';
        if (m.phase === 'HAS_SCHEDULE') {
            statusLine = '<p class="text-xs text-gray-600 mt-2"><i class="fas fa-clock text-green-600"></i> '
                + escapeHtml(m.upcomingShowtimes) + ' suất sắp tới</p>';
        } else if (m.phase === 'WAITING_SCHEDULE') {
            statusLine = '<p class="text-xs text-gray-600 mt-2"><i class="fas fa-calendar-plus text-amber-600"></i> Chưa có suất</p>';
        } else if (m.phase === 'ENDED') {
            statusLine = '<p class="text-xs text-gray-600 mt-2"><i class="fas fa-history"></i> '
                + escapeHtml(m.totalShowtimes) + ' suất đã từng chiếu</p>';
        }

        return '<article class="movie-card bg-white rounded-lg shadow overflow-hidden" data-movie-id="' + m.movieId + '">'
            + poster
            + '<div class="p-3 flex-1 flex flex-col">'
            + '<span class="phase-badge mb-2 self-start ' + pm.css + '"><i class="fas ' + pm.icon + '"></i><span>'
            + escapeHtml(pm.label) + '</span></span>'
            + '<h2 class="font-semibold text-sm leading-snug line-clamp-2">' + escapeHtml(m.displayTitle) + '</h2>'
            + '<p class="text-xs text-gray-500 mt-1">' + escapeHtml(formatMeta(m)) + '</p>'
            + statusLine
            + bookingLine
            + '<div class="mt-auto pt-3 flex flex-wrap gap-2 text-xs border-t border-gray-100 movie-card__actions">'
            + '<a href="/admin/movies/' + m.movieId + '/showtimes" class="text-blue-600 hover:underline">Suất</a>'
            + '<a href="/admin/movies/' + m.movieId + '/edit?lang=' + encodeURIComponent(lang) + '" class="text-primary hover:underline">Sửa</a>'
            + scheduleBtn + hideBtn
            + '</div></div></article>';
    }

    async function fetchPosters(movieIds) {
        if (!movieIds.length) return {};
        const params = new URLSearchParams();
        movieIds.forEach(function (id) { params.append('movieIds', String(id)); });
        params.set('lang', lang);
        const res = await fetch('/admin/movies/api/posters?' + params.toString(), {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin'
        });
        if (!res.ok) return {};
        return res.json();
    }

    function applyPosters(grid, items) {
        const missing = items.filter(function (m) { return !m.posterUrl; }).map(function (m) { return m.movieId; });
        if (!missing.length) return;
        fetchPosters(missing).then(function (map) {
            Object.keys(map).forEach(function (id) {
                const card = grid.querySelector('[data-movie-id="' + id + '"]');
                if (!card) return;
                const ph = card.querySelector('.movie-card__poster-placeholder');
                if (ph && map[id]) {
                    const img = document.createElement('img');
                    img.src = map[id];
                    img.alt = '';
                    img.className = 'movie-card__poster';
                    img.loading = 'lazy';
                    ph.replaceWith(img);
                }
            });
        }).catch(function () { /* ignore */ });
    }

    async function loadSection(sectionEl, page) {
        const phase = sectionEl.dataset.phase;
        const grid = sectionEl.querySelector('.section-grid');
        const loadingEl = sectionEl.querySelector('.section-loading');
        const emptyEl = sectionEl.querySelector('.section-empty');
        const countEl = sectionEl.querySelector('.section-count');
        const moreLoading = sectionEl.querySelector('.section-more-loading');
        const endEl = sectionEl.querySelector('.section-end');
        const sentinel = sectionEl.querySelector('.section-sentinel');

        if (page === 0) {
            grid.innerHTML = skeletonCards(4);
            if (loadingEl) loadingEl.classList.remove('hidden');
            if (emptyEl) emptyEl.classList.add('hidden');
            if (endEl) endEl.classList.add('hidden');
            if (sentinel) sentinel.classList.remove('hidden');
        } else if (moreLoading) {
            moreLoading.classList.remove('hidden');
        }

        const params = new URLSearchParams({ phase: phase, lang: lang, page: String(page), q: keyword });
        const res = await fetch('/admin/movies/api/section?' + params.toString(), {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin'
        });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();

        if (loadingEl) loadingEl.classList.add('hidden');
        if (moreLoading) moreLoading.classList.add('hidden');

        if (page === 0) {
            grid.innerHTML = '';
        }

        if (!data.items || !data.items.length) {
            if (page === 0) {
                emptyEl.textContent = 'Không có phim trong nhóm này.';
                emptyEl.classList.remove('hidden');
            }
        } else {
            emptyEl.classList.add('hidden');
            grid.insertAdjacentHTML('beforeend', data.items.map(buildCard).join(''));
            applyPosters(grid, data.items);
        }

        const total = data.totalElements != null ? data.totalElements : 0;
        countEl.textContent = total + ' phim';
        if (phase === 'WAITING_SCHEDULE') {
            countEl.textContent += ' · cuộn để tải thêm (20/lần)';
        } else if (phase === 'HAS_SCHEDULE') {
            countEl.textContent += ' · có suất sắp tới';
        }

        sectionEl._page = page + 1;
        sectionEl._hasNext = data.hasNext;

        if (!data.hasNext) {
            if (endEl) {
                endEl.textContent = 'Đã hiển thị tất cả.';
                endEl.classList.remove('hidden');
            }
            if (sentinel) sentinel.classList.add('hidden');
        }
    }

    function initSection(sectionEl) {
        sectionEl._page = 0;
        sectionEl._hasNext = true;
        sectionEl._loading = false;

        loadSection(sectionEl, 0).catch(function (e) {
            console.error(e);
            const loading = sectionEl.querySelector('.section-loading');
            if (loading) {
                loading.textContent = 'Không tải được dữ liệu.';
            }
        });

        const sentinel = sectionEl.querySelector('.section-sentinel');
        if (sentinel && sectionEl.dataset.phase === 'WAITING_SCHEDULE') {
            const observer = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (entry.isIntersecting && sectionEl._hasNext && !sectionEl._loading) {
                        sectionEl._loading = true;
                        loadSection(sectionEl, sectionEl._page)
                            .catch(function (err) { console.error(err); })
                            .finally(function () { sectionEl._loading = false; });
                    }
                });
            }, { rootMargin: '200px' });
            observer.observe(sentinel);
        }
    }

    const sections = Array.from(document.querySelectorAll('.movie-section[data-phase]'));
    const visible = hubPhase
        ? sections.filter(function (s) { return s.dataset.phase === hubPhase; })
        : sections;

    visible.forEach(initSection);
})();
