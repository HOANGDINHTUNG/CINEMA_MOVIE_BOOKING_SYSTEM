(function () {
    'use strict';

    function escapeHtml(text) {
        if (text == null) return '';
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    function formatDateTime(iso) {
        if (!iso) return '';
        const d = new Date(iso);
        if (isNaN(d.getTime())) return iso;
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function formatPrice(n) {
        if (n == null) return '';
        return Number(n).toLocaleString('vi-VN') + 'đ';
    }

    function initListPage() {
        const body = document.body;
        if (!body.classList.contains('admin-showtimes-list-page')) return;

        const tbody = document.getElementById('showtimes-tbody');
        const loading = document.getElementById('showtimes-loading');
        const pagination = document.getElementById('showtimes-pagination');
        let currentPage = parseInt(body.dataset.page || '0', 10);

        function buildQuery(page) {
            const params = new URLSearchParams();
            params.set('page', String(page));
            if (body.dataset.movieId) params.set('movieId', body.dataset.movieId);
            if (body.dataset.roomId) params.set('roomId', body.dataset.roomId);
            if (body.dataset.status) params.set('status', body.dataset.status);
            if (body.dataset.fromDate) params.set('fromDate', body.dataset.fromDate);
            if (body.dataset.toDate) params.set('toDate', body.dataset.toDate);
            return params;
        }

        function renderPagination(data) {
            if (!data || data.totalElements <= data.size) {
                pagination.classList.add('hidden');
                return;
            }
            const totalPages = Math.max(1, Math.ceil(data.totalElements / data.size));
            const pageNum = data.page + 1;
            let html = '';
            if (data.page > 0) {
                html += '<button type="button" data-page="' + (data.page - 1) + '" class="px-3 py-1 border rounded bg-white">Trước</button>';
            }
            html += '<span>Trang ' + pageNum + ' / ' + totalPages + '</span>';
            if (data.hasNext) {
                html += '<button type="button" data-page="' + (data.page + 1) + '" class="px-3 py-1 border rounded bg-white">Sau</button>';
            }
            pagination.innerHTML = html;
            pagination.classList.remove('hidden');
            pagination.querySelectorAll('button[data-page]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    load(parseInt(btn.getAttribute('data-page'), 10));
                });
            });
        }

        function load(page) {
            currentPage = page;
            loading.classList.remove('hidden');
            tbody.innerHTML = '';
            fetch('/admin/showtimes/api/list?' + buildQuery(page).toString(), {
                headers: { Accept: 'application/json' },
                credentials: 'same-origin'
            })
                .then(function (res) {
                    if (!res.ok) throw new Error('HTTP ' + res.status);
                    return res.json();
                })
                .then(function (data) {
                    loading.classList.add('hidden');
                    if (!data.items || !data.items.length) {
                        tbody.innerHTML = '<tr><td colspan="8" class="px-4 py-8 text-center text-gray-500">Không có suất phù hợp bộ lọc.</td></tr>';
                    } else {
                        tbody.innerHTML = data.items.map(function (s) {
                            return '<tr class="hover:bg-gray-50">'
                                + '<td class="px-4 py-3">' + escapeHtml(s.showtimeId) + '</td>'
                                + '<td class="px-4 py-3">' + escapeHtml(s.movieTitle) + '</td>'
                                + '<td class="px-4 py-3">' + escapeHtml(s.roomName) + '</td>'
                                + '<td class="px-4 py-3">' + escapeHtml(formatDateTime(s.startTime)) + '</td>'
                                + '<td class="px-4 py-3">' + escapeHtml(formatPrice(s.basePrice)) + '</td>'
                                + '<td class="px-4 py-3"><span>' + escapeHtml(s.fillPercent) + '%</span> '
                                + '<span class="text-gray-400 text-xs">(' + escapeHtml(s.bookedSeats) + '/' + escapeHtml(s.totalSeats) + ')</span></td>'
                                + '<td class="px-4 py-3">' + escapeHtml(s.status) + '</td>'
                                + '<td class="px-4 py-3 text-right"><a href="/admin/showtimes/' + s.showtimeId + '" class="text-primary hover:underline">Chi tiết</a></td>'
                                + '</tr>';
                        }).join('');
                    }
                    renderPagination(data);
                })
                .catch(function (e) {
                    console.error(e);
                    loading.textContent = 'Không tải được danh sách suất.';
                });
        }

        load(currentPage);
    }

    function initCalendarPage() {
        const body = document.body;
        if (!body.classList.contains('admin-showtimes-calendar-page')) return;

        const container = document.getElementById('calendar-container');
        const loading = document.getElementById('calendar-loading');
        const empty = document.getElementById('calendar-empty');
        const weekLabel = document.getElementById('calendar-week-label');

        const params = new URLSearchParams();
        if (body.dataset.week) params.set('week', body.dataset.week);
        if (body.dataset.roomId) params.set('roomId', body.dataset.roomId);

        fetch('/admin/showtimes/api/calendar?' + params.toString(), {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin'
        })
            .then(function (res) {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(function (cal) {
                loading.classList.add('hidden');
                if (!cal.rooms || !cal.rooms.length) {
                    empty.classList.remove('hidden');
                    return;
                }
                if (weekLabel && cal.weekStart && cal.weekEnd) {
                    weekLabel.textContent = formatLocalDate(cal.weekStart) + ' – ' + formatLocalDate(cal.weekEnd);
                }
                container.innerHTML = renderCalendarTable(cal);
                container.classList.remove('hidden');
            })
            .catch(function (e) {
                console.error(e);
                loading.textContent = 'Không tải được lịch tuần.';
            });

        function formatLocalDate(iso) {
            if (!iso) return '';
            const parts = String(iso).split('-');
            if (parts.length === 3) return parts[2] + '/' + parts[1] + '/' + parts[0];
            return iso;
        }

        function dayHeader(d) {
            const date = new Date(d + 'T12:00:00');
            const weekdays = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
            const label = weekdays[date.getDay()];
            const parts = String(d).split('-');
            const short = parts.length === 3 ? parts[2] + '/' + parts[1] : d;
            return '<span>' + escapeHtml(label) + '</span><br/><span class="text-gray-500 font-normal">' + escapeHtml(short) + '</span>';
        }

        function renderCalendarTable(cal) {
            let html = '<table class="admin-calendar-table w-full text-sm"><thead><tr>'
                + '<th class="admin-calendar-room-col text-left">Phòng</th>';
            (cal.days || []).forEach(function (day) {
                html += '<th>' + dayHeader(day) + '</th>';
            });
            html += '</tr></thead><tbody>';

            (cal.rooms || []).forEach(function (room) {
                html += '<tr><td class="admin-calendar-room-col">' + escapeHtml(room.roomName) + '</td>';
                (cal.days || []).forEach(function (day) {
                    const dayKey = String(day);
                    const events = (cal.grid && cal.grid[room.roomId] && cal.grid[room.roomId][dayKey]) || [];
                    html += '<td class="admin-calendar-cell">';
                    events.forEach(function (ev) {
                        const time = ev.startTime ? String(ev.startTime).substring(11, 16) : '';
                        html += '<a href="/admin/showtimes/' + ev.showtimeId + '" class="admin-cal-event status-' + escapeHtml(ev.status) + '" title="' + escapeHtml(ev.movieTitle) + '">'
                            + '<span class="font-semibold">' + escapeHtml(time) + '</span>'
                            + '<span class="block truncate">' + escapeHtml(ev.movieTitle) + '</span>'
                            + '<span class="text-gray-500">' + escapeHtml(ev.fillPercent) + '%</span></a>';
                    });
                    const addStart = dayKey + 'T10:00:00';
                    html += '<a href="/admin/showtimes/new?roomId=' + room.roomId + '&startTime=' + encodeURIComponent(addStart) + '" class="admin-cal-add" title="Thêm suất chiếu">'
                        + '<i class="fas fa-plus"></i> Thêm</a>';
                    html += '</td>';
                });
                html += '</tr>';
            });
            html += '</tbody></table>';
            return html;
        }
    }

    initListPage();
    initCalendarPage();
})();
