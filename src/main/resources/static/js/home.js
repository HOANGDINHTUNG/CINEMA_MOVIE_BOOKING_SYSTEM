(function () {
    const body = document.body;
    if (!body || !body.dataset.appLang) {
        return;
    }

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
})();
