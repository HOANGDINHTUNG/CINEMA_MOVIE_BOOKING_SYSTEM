(function () {
    'use strict';

    const section = document.getElementById('section-waiting');
    const grid = document.getElementById('waiting-grid');
    const sentinel = document.getElementById('waiting-sentinel');
    const loadingEl = document.getElementById('waiting-loading');
    const endEl = document.getElementById('waiting-end');
    const emptyEl = document.getElementById('waiting-empty');
    const body = document.body;

    if (!section || !grid || !sentinel) {
        return;
    }

    const lang = body.dataset.lang || 'vi-VN';
    const keyword = body.dataset.keyword || '';
    const csrfParam = body.dataset.csrfParam || '';
    const csrfToken = body.dataset.csrfToken || '';

    let page = parseInt(section.dataset.initialPage || '0', 10);
    let hasNext = section.dataset.hasNext === 'true';
    let loading = false;

    function escapeHtml(text) {
        if (text == null) return '';
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    function formatMeta(m) {
        const parts = ['#' + m.movieId];
        if (m.tmdbId) parts.push('TMDB ' + m.tmdbId);
        if (m.duration) parts.push(m.duration + 'p');
        return parts.join(' · ');
    }

    function buildCard(m) {
        const poster = m.posterUrl
            ? '<img src="' + escapeHtml(m.posterUrl) + '" alt="" class="movie-card__poster" loading="lazy"/>'
            : '<div class="movie-card__poster-placeholder"><i class="fas fa-film"></i></div>';
        const scheduleBtn = m.phase === 'WAITING_SCHEDULE'
            ? '<form action="/admin/movies/' + m.movieId + '/regenerate-schedule" method="post" class="inline">'
            + '<input type="hidden" name="' + escapeHtml(csrfParam) + '" value="' + escapeHtml(csrfToken) + '"/>'
            + '<button type="submit" class="text-amber-700 hover:underline" onclick="return confirm(\'Tạo lịch mẫu 10 ngày cho phim này?\')">Tạo lịch mẫu</button></form>'
            : '';
        const hideBtn = m.status === 'ACTIVE'
            ? '<form action="/admin/movies/' + m.movieId + '/delete" method="post" class="inline">'
            + '<input type="hidden" name="' + escapeHtml(csrfParam) + '" value="' + escapeHtml(csrfToken) + '"/>'
            + '<button type="submit" class="text-red-600 hover:underline" onclick="return confirm(\'Ẩn phim khỏi rạp?\')">Ẩn</button></form>'
            : '';

        return '<article class="movie-card bg-white rounded-lg shadow overflow-hidden" data-movie-id="' + m.movieId + '">'
            + poster
            + '<div class="p-3 flex-1 flex flex-col">'
            + '<span class="phase-badge mb-2 self-start phase-waiting"><i class="fas fa-hourglass-half"></i><span>Đang đợi lịch chiếu</span></span>'
            + '<h2 class="font-semibold text-sm leading-snug line-clamp-2">' + escapeHtml(m.displayTitle) + '</h2>'
            + '<p class="text-xs text-gray-500 mt-1">' + escapeHtml(formatMeta(m)) + '</p>'
            + '<p class="text-xs text-gray-600 mt-2"><i class="fas fa-calendar-plus text-amber-600"></i> Chưa có suất — cần xếp lịch</p>'
            + '<div class="mt-auto pt-3 flex flex-wrap gap-2 text-xs border-t border-gray-100 movie-card__actions">'
            + '<a href="/admin/movies/' + m.movieId + '/showtimes" class="text-blue-600 hover:underline">Suất</a>'
            + '<a href="/admin/movies/' + m.movieId + '/edit?lang=' + encodeURIComponent(lang) + '" class="text-primary hover:underline">Sửa</a>'
            + scheduleBtn + hideBtn
            + '</div></div></article>';
    }

    function appendItems(items) {
        if (!items || !items.length) return;
        if (emptyEl) emptyEl.classList.add('hidden');
        grid.classList.remove('hidden');
        grid.insertAdjacentHTML('beforeend', items.map(buildCard).join(''));
    }

    function setLoading(on) {
        loading = on;
        if (loadingEl) loadingEl.classList.toggle('hidden', !on);
    }

    function showEnd() {
        hasNext = false;
        if (endEl) endEl.classList.remove('hidden');
        if (sentinel) sentinel.classList.add('hidden');
    }

    async function loadMore() {
        if (loading || !hasNext) return;
        setLoading(true);
        try {
            const params = new URLSearchParams({ lang, page: String(page), q: keyword });
            const res = await fetch('/admin/movies/api/waiting-schedule?' + params.toString(), {
                headers: { Accept: 'application/json' },
                credentials: 'same-origin'
            });
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();
            appendItems(data.items);
            hasNext = data.hasNext;
            page += 1;
            if (!hasNext) showEnd();
        } catch (e) {
            console.error('Load waiting-schedule movies failed', e);
        } finally {
            setLoading(false);
        }
    }

    const observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting && hasNext && !loading) {
                loadMore();
            }
        });
    }, { root: null, rootMargin: '240px 0px', threshold: 0 });

    observer.observe(sentinel);

    if (!hasNext) {
        showEnd();
    }

    const hash = window.location.hash;
    if (hash) {
        const target = document.querySelector(hash);
        if (target) {
            requestAnimationFrame(function () {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
        }
    }
})();
