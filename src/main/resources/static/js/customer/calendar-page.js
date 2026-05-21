(function () {
    'use strict';

    const body = document.body;
    if (!body || !body.classList.contains('schedule-page--async')) {
        return;
    }

    const cfg = {
        lang: body.dataset.appLang || 'vi-VN',
        countryLabels: parseJson(body.dataset.countryLabels, {}),
        msgLoading: body.dataset.msgLoading || 'Đang tải…',
        msgLoadError: body.dataset.msgLoadError || 'Không tải được lịch chiếu.',
        msgResultsCountTpl: body.dataset.msgResultsCountTpl || 'Kết quả: __N__ phim',
        msgResultsDateTpl: body.dataset.msgResultsDateTpl || 'Ngày __D__',
        msgEmptyFiltered: body.dataset.msgEmptyFiltered || '',
        msgEmptyDay: body.dataset.msgEmptyDay || '',
        msgViewDetail: body.dataset.msgViewDetail || 'Xem chi tiết',
        msgShowtimeList: body.dataset.msgShowtimeList || 'Lịch chiếu',
        msgOrigin: body.dataset.msgOrigin || 'Xuất xứ',
        msgRelease: body.dataset.msgRelease || 'Khởi chiếu',
        msgMinutes: body.dataset.msgMinutes || 'phút',
        msgBookSeatsTpl: body.dataset.msgBookSeatsTpl || 'Đặt vé suất __T__',
        msgSoldOut: body.dataset.msgSoldOut || 'Hết vé',
        msgFilterAvailable: body.dataset.msgFilterAvailable || 'Còn vé',
        msgSearchAll: body.dataset.msgSearchAll || 'Tất cả'
    };

    const moviesRoot = document.getElementById('scheduleMoviesRoot');
    const dayTabs = document.getElementById('scheduleDayTabs');
    const resultsCountEl = document.querySelector('#scheduleResultsSummary .schedule-results-bar__count');
    const resultsDateEl = document.getElementById('scheduleResultsDate');
    const summaryBar = document.getElementById('scheduleSummaryBar');
    const summaryChips = document.getElementById('scheduleSummaryChips');
    const filterForm = document.getElementById('scheduleFilterForm');
    const sortForm = document.getElementById('scheduleSortForm');
    const genreSelect = document.getElementById('searchGenre');
    const viewToggle = document.getElementById('scheduleViewToggle');

    let loadSeq = 0;
    let genresLoaded = false;

    hidePageLoaderNow();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }

    function boot() {
        bindFilterForm();
        bindSortForm();
        bindViewToggle();
        loadScheduleFromUrl();
    }

    function hidePageLoaderNow() {
        const loader = document.getElementById('pageLoader');
        if (!loader) return;
        loader.dataset.hidden = 'true';
        loader.style.opacity = '0';
        loader.style.pointerEvents = 'none';
        loader.style.display = 'none';
    }

    function parseJson(raw, fallback) {
        if (!raw) return fallback;
        try {
            return JSON.parse(raw);
        } catch (e) {
            return fallback;
        }
    }

    function bindFilterForm() {
        if (!filterForm) return;
        filterForm.addEventListener('submit', function (event) {
            event.preventDefault();
            syncUrlAndLoad();
            closeMobileFilters();
        });
    }

    function bindSortForm() {
        if (!sortForm) return;
        const sortSelect = document.getElementById('scheduleSort');
        if (sortSelect) {
            sortSelect.addEventListener('change', function () {
                syncUrlAndLoad();
            });
        }
    }

    function bindViewToggle() {
        if (!viewToggle) return;
        viewToggle.querySelectorAll('[data-schedule-view]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                const mode = btn.getAttribute('data-schedule-view') || 'grid';
                const viewInput = filterForm && filterForm.querySelector('input[name="view"]');
                if (viewInput) {
                    viewInput.value = mode;
                }
                if (sortForm) {
                    const sv = sortForm.querySelector('input[name="view"]');
                    if (sv) sv.value = mode;
                }
                viewToggle.querySelectorAll('.schedule-view-toggle__btn').forEach(function (b) {
                    b.classList.toggle('is-active', b === btn);
                });
                if (moviesRoot) {
                    moviesRoot.classList.toggle('movies--list', mode === 'list');
                    moviesRoot.classList.toggle('movies--grid', mode !== 'list');
                }
                syncUrlAndLoad(false);
            });
        });
    }

    function closeMobileFilters() {
        const sidebar = document.getElementById('scheduleSidebar');
        const backdrop = document.getElementById('scheduleSidebarBackdrop');
        if (sidebar) sidebar.classList.remove('is-open');
        if (backdrop) backdrop.hidden = true;
        document.body.classList.remove('schedule-filters-open');
    }

    function readParamsFromForms() {
        const params = new URLSearchParams(window.location.search);
        if (filterForm) {
            const fd = new FormData(filterForm);
            params.delete('q');
            params.delete('genre');
            params.delete('date');
            params.delete('sort');
            params.delete('view');
            params.delete('age');
            params.delete('format');
            params.delete('origin');
            params.delete('available');
            fd.forEach(function (value, key) {
                if (value == null || String(value).trim() === '') return;
                if (key === 'available' && value !== 'true') return;
                params.append(key, value);
            });
        }
        if (!params.has('date')) {
            params.set('date', body.dataset.selectedDate || todayIso());
        }
        return params;
    }

    function todayIso() {
        const d = new Date();
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
    }

    function syncUrlAndLoad(pushHistory) {
        const params = readParamsFromForms();
        const qs = params.toString();
        const path = window.location.pathname + (qs ? '?' + qs : '');
        if (pushHistory !== false) {
            window.history.pushState(null, '', path);
        }
        loadSchedule(params);
    }

    function loadScheduleFromUrl() {
        const params = new URLSearchParams(window.location.search);
        if (!params.has('date')) {
            params.set('date', body.dataset.selectedDate || todayIso());
        }
        loadSchedule(params);
    }

    window.addEventListener('popstate', function () {
        loadScheduleFromUrl();
        syncFormsFromUrl();
    });

    function syncFormsFromUrl() {
        const params = new URLSearchParams(window.location.search);
        if (!filterForm) return;
        setField('q', params.get('q') || '');
        setField('genre', params.get('genre') || '');
        setField('date', params.get('date') || body.dataset.selectedDate || '');
        setField('sort', params.get('sort') || 'name');
        setField('view', params.get('view') || 'grid');
        const avail = filterForm.querySelector('input[name="available"]');
        if (avail) avail.checked = params.get('available') === 'true';
        syncCheckboxGroup('age', params.getAll('age'));
        syncCheckboxGroup('format', params.getAll('format'));
        const origin = params.get('origin') || params.getAll('origin')[0] || '';
        const originEl = filterForm.querySelector('[name="origin"]');
        if (originEl) originEl.value = origin;
    }

    function setField(name, value) {
        const el = filterForm.querySelector('[name="' + name + '"]');
        if (el) el.value = value;
        if (sortForm) {
            const s = sortForm.querySelector('[name="' + name + '"]');
            if (s) s.value = value;
        }
    }

    function syncCheckboxGroup(name, values) {
        filterForm.querySelectorAll('input[name="' + name + '"]').forEach(function (cb) {
            cb.checked = values.indexOf(cb.value) >= 0;
        });
    }

    async function loadSchedule(params) {
        const seq = ++loadSeq;
        setLoadingState(true);
        params.set('lang', cfg.lang);

        try {
            const res = await fetch('/api/public/schedule?' + params.toString(), {
                headers: { Accept: 'application/json' }
            });
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();
            if (seq !== loadSeq) return;

            if (!genresLoaded) {
                populateGenres(data.genres || [], params.get('genre') || '');
                genresLoaded = true;
            }

            renderDayTabs(data.days || [], params);
            renderMovies(data.movies || [], params.get('view') || 'grid');
            renderSummary(data, params);
            updateResultsBar(data);
            syncHiddenFormFields(data.selectedDate, params);
        } catch (err) {
            console.error(err);
            if (seq !== loadSeq) return;
            if (moviesRoot) {
                moviesRoot.innerHTML = '<p class="schedule-empty schedule-empty--error">' + escapeHtml(cfg.msgLoadError) + '</p>';
                moviesRoot.setAttribute('aria-busy', 'false');
            }
        } finally {
            if (seq === loadSeq) {
                setLoadingState(false);
            }
        }
    }

    function setLoadingState(loading) {
        if (moviesRoot) {
            moviesRoot.setAttribute('aria-busy', loading ? 'true' : 'false');
            moviesRoot.classList.toggle('is-loading', loading);
        }
        if (loading && resultsCountEl) {
            resultsCountEl.textContent = cfg.msgLoading;
        }
    }

    function populateGenres(genres, selected) {
        if (!genreSelect) return;
        const keep = genreSelect.querySelector('option[value=""]');
        genreSelect.innerHTML = '';
        if (keep) {
            genreSelect.appendChild(keep);
        } else {
            const opt = document.createElement('option');
            opt.value = '';
            opt.textContent = cfg.msgSearchAll;
            genreSelect.appendChild(opt);
        }
        genres.forEach(function (g) {
            const opt = document.createElement('option');
            opt.value = String(g.id);
            opt.textContent = g.name || '';
            if (selected && selected === String(g.id)) {
                opt.selected = true;
            }
            genreSelect.appendChild(opt);
        });
    }

    function renderDayTabs(days, params) {
        if (!dayTabs) return;
        dayTabs.innerHTML = '';
        days.forEach(function (day) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'date-btn' + (day.selected ? ' active' : '');
            btn.textContent = day.label || '';
            btn.addEventListener('click', function () {
                if (day.selected) return;
                const dateStr = normalizeIsoDate(day.date);
                const p = readParamsFromForms();
                p.set('date', dateStr);
                window.history.pushState(null, '', window.location.pathname + '?' + p.toString());
                syncFormsFromUrl();
                loadSchedule(p);
            });
            dayTabs.appendChild(btn);
        });
    }

    function renderMovies(movies, viewMode) {
        if (!moviesRoot) return;
        const mode = viewMode === 'list' ? 'list' : 'grid';
        moviesRoot.className = 'movies ' + (mode === 'list' ? 'movies--list' : 'movies--grid');
        if (!movies.length) {
            const filtering = hasActiveFilters(readParamsFromForms());
            moviesRoot.innerHTML = '<p class="schedule-empty"><span>'
                + escapeHtml(filtering ? cfg.msgEmptyFiltered : cfg.msgEmptyDay) + '</span></p>';
            return;
        }
        const html = movies.map(function (card) { return buildMovieCardHtml(card); }).join('');
        moviesRoot.innerHTML = html;
    }

    function buildMovieCardHtml(card) {
        const tmdbId = card.tmdbId;
        const detailUrl = '/customer/movies/' + tmdbId;
        const poster = posterUrl(card.posterUrl);
        const genres = card.genresLabel && card.genresLabel !== '—' ? escapeHtml(card.genresLabel) : '';
        const duration = card.duration != null
            ? ', <span>' + card.duration + ' ' + escapeHtml(cfg.msgMinutes) + '</span>' : '';
        const catTitle = (genres + duration).replace(/^, /, '');

        let slotsHtml = '';
        (card.slots || []).forEach(function (slot) {
            if (slot.soldOut) {
                slotsHtml += '<span class="showtime-btn showtime-btn--sold-out" aria-label="'
                    + escapeHtml(cfg.msgSoldOut) + '">' + escapeHtml(slot.timeLabel || '') + '</span>';
            } else {
                const label = tpl(cfg.msgBookSeatsTpl, { __T__: slot.timeLabel || '' });
                slotsHtml += '<a class="showtime-btn" href="/customer/showtimes/'
                    + slot.showtimeId + '/seats" aria-label="' + escapeHtml(label) + '">'
                    + escapeHtml(slot.timeLabel || '') + '</a>';
            }
        });

        let metaOrigin = '';
        if (card.originLabel) {
            metaOrigin = '<p class="schedule-meta schedule-clip schedule-clip--1" title="'
                + escapeHtml(cfg.msgOrigin + ': ' + card.originLabel) + '"><span>'
                + escapeHtml(cfg.msgOrigin) + '</span>: <strong>' + escapeHtml(card.originLabel) + '</strong></p>';
        }

        let metaRelease = '';
        if (card.releaseDate) {
            const rd = formatDate(card.releaseDate);
            metaRelease = '<p class="schedule-meta schedule-clip schedule-clip--1" title="'
                + escapeHtml(cfg.msgRelease + ': ' + rd) + '"><span>'
                + escapeHtml(cfg.msgRelease) + '</span>: <strong>' + escapeHtml(rd) + '</strong></p>';
        }

        const formatBadge = card.format
            ? '<span class="poster-badge poster-badge--format">' + escapeHtml(card.format) + '</span>' : '';
        const ageBadge = card.ageLabel
            ? '<span class="poster-badge poster-badge--age">' + escapeHtml(card.ageLabel) + '</span>' : '';

        return '<article class="movie-card">'
            + '<a href="' + detailUrl + '" class="poster-link" title="' + escapeHtml(card.title || '') + '">'
            + '<span class="poster-frame"><img src="' + escapeHtml(poster) + '" alt="" loading="lazy"/></span>'
            + formatBadge + ageBadge + '</a>'
            + '<div class="info"><div class="top">'
            + '<div class="category-time schedule-clip schedule-clip--1" title="' + escapeHtml(catTitle) + '">'
            + (genres ? '<span>' + genres + '</span>' : '') + duration
            + '</div><span class="format">' + escapeHtml(card.format || '') + '</span></div>'
            + '<div class="title-format"><h3 class="schedule-clip schedule-clip--1">'
            + '<a href="' + detailUrl + '">' + escapeHtml(card.title || '') + '</a></h3>'
            + '<span class="format-1">' + escapeHtml(card.format || '') + '</span></div>'
            + '<a class="schedule-detail-link" href="' + detailUrl + '">' + escapeHtml(cfg.msgViewDetail) + '</a>'
            + metaOrigin + metaRelease
            + '<p class="age schedule-clip schedule-clip--2" title="' + escapeHtml(card.ageNote || '') + '">'
            + escapeHtml(card.ageNote || '') + '</p>'
            + '<p class="showtime-label"><b>' + escapeHtml(cfg.msgShowtimeList) + '</b></p>'
            + '<div class="showtimes">' + slotsHtml + '</div></div></article>';
    }

    function renderSummary(data, params) {
        if (!summaryBar || !summaryChips) return;
        if (!data.filtering) {
            summaryBar.hidden = true;
            summaryChips.innerHTML = '';
            return;
        }
        summaryBar.hidden = false;
        summaryChips.innerHTML = '';
        const q = params.get('q');
        if (q) addChip(q);
        if (data.selectedGenreName) addChip(data.selectedGenreName);
        params.getAll('age').forEach(addChip);
        params.getAll('format').forEach(addChip);
        const origin = params.get('origin') || '';
        if (origin) addChip(cfg.countryLabels[origin] || origin);
        if (params.get('available') === 'true') addChip(cfg.msgFilterAvailable);

        function addChip(text) {
            const span = document.createElement('span');
            span.className = 'schedule-summary-bar__chip';
            span.textContent = text;
            summaryChips.appendChild(span);
        }
    }

    function updateResultsBar(data) {
        if (resultsCountEl) {
            resultsCountEl.textContent = tpl(cfg.msgResultsCountTpl, { __N__: String(data.resultCount != null ? data.resultCount : 0) });
        }
        if (resultsDateEl && data.selectedDate) {
            resultsDateEl.textContent = tpl(cfg.msgResultsDateTpl, { __D__: formatDate(data.selectedDate) });
        }
    }

    function syncHiddenFormFields(selectedDate, params) {
        if (!selectedDate) return;
        const iso = String(selectedDate).substring(0, 10);
        setField('date', iso);
        body.dataset.selectedDate = iso;
    }

    function hasActiveFilters(params) {
        return !!(params.get('q') || params.get('genre') || params.get('available') === 'true'
            || params.getAll('age').length || params.getAll('format').length
            || (params.get('origin') || '').length);
    }

    function posterUrl(url) {
        if (url && url.indexOf('http') === 0) return url;
        if (url && url.length) return url.startsWith('/') ? url : '/' + url;
        return '/assets/img/poster.jpg';
    }

    function normalizeIsoDate(value) {
        if (!value) return todayIso();
        if (typeof value === 'string') return value.substring(0, 10);
        if (Array.isArray(value) && value.length >= 3) {
            const pad = function (n) { return String(n).padStart(2, '0'); };
            return value[0] + '-' + pad(value[1]) + '-' + pad(value[2]);
        }
        return String(value).substring(0, 10);
    }

    function formatDate(iso) {
        const normalized = normalizeIsoDate(iso);
        if (!normalized) return '';
        const parts = normalized.split('-');
        if (parts.length !== 3) return iso;
        return parts[2] + '/' + parts[1] + '/' + parts[0];
    }

    function tpl(template, map) {
        let s = template || '';
        Object.keys(map).forEach(function (k) {
            s = s.split(k).join(String(map[k]));
        });
        return s;
    }

    function escapeHtml(text) {
        if (text == null) return '';
        const el = document.createElement('div');
        el.textContent = String(text);
        return el.innerHTML;
    }
})();
