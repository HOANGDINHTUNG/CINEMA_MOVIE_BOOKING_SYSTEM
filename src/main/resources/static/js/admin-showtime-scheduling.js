(function () {
    'use strict';

    function todayIso() {
        const d = new Date();
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
    }

    function nowIsoLocal() {
        const d = new Date();
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
            + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function parseTimeToken(token) {
        if (!token) return null;
        const t = token.trim();
        const m = /^(\d{1,2}):(\d{2})$/.exec(t);
        if (!m) return null;
        const h = parseInt(m[1], 10);
        const min = parseInt(m[2], 10);
        if (h < 0 || h > 23 || min < 0 || min > 59) return null;
        return { h: h, min: min };
    }

    function validateDateRange(startVal, endVal, requireNotPast) {
        const today = todayIso();
        if (!startVal || !endVal) {
            return { ok: false, message: 'Chọn đủ Từ ngày và Đến ngày.' };
        }
        if (requireNotPast && startVal < today) {
            return { ok: false, message: 'Từ ngày không được trước hôm nay.' };
        }
        if (requireNotPast && endVal < today) {
            return { ok: false, message: 'Đến ngày không được trước hôm nay.' };
        }
        if (endVal < startVal) {
            return { ok: false, message: 'Đến ngày phải sau hoặc bằng Từ ngày.' };
        }
        return { ok: true, message: '' };
    }

    function validateBulkTimeSlots(startDate, endDate, timeSlotsRaw) {
        const range = validateDateRange(startDate, endDate, true);
        if (!range.ok) return range;
        const parts = (timeSlotsRaw || '').split(/[,;\s]+/).filter(Boolean);
        if (parts.length === 0) {
            return { ok: false, message: 'Nhập ít nhất một khung giờ (HH:mm).' };
        }
        const now = new Date();
        const past = [];
        for (let d = new Date(startDate + 'T00:00:00'); ; d.setDate(d.getDate() + 1)) {
            const dayIso = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0')
                + '-' + String(d.getDate()).padStart(2, '0');
            if (dayIso > endDate) break;
            for (let i = 0; i < parts.length; i++) {
                const parsed = parseTimeToken(parts[i]);
                if (!parsed) {
                    return { ok: false, message: 'Khung giờ không hợp lệ: «' + parts[i] + '». Dùng HH:mm.' };
                }
                const slotDate = new Date(dayIso + 'T' + String(parsed.h).padStart(2, '0')
                    + ':' + String(parsed.min).padStart(2, '0') + ':00');
                if (slotDate.getTime() <= now.getTime()) {
                    past.push(dayIso + ' ' + String(parsed.h).padStart(2, '0') + ':' + String(parsed.min).padStart(2, '0'));
                }
            }
        }
        if (past.length > 0) {
            const preview = past.slice(0, 5).join(', ');
            return {
                ok: false,
                message: 'Có khung giờ đã qua: ' + preview + (past.length > 5 ? ' …' : '')
                    + '. Chọn lại ngày hoặc giờ từ hiện tại.'
            };
        }
        return { ok: true, message: '' };
    }

    function bindBulkDateValidation(form) {
        const startInput = form.querySelector('#bulkStartDate') || form.querySelector('[name=startDate]');
        const endInput = form.querySelector('#bulkEndDate') || form.querySelector('[name=endDate]');
        const timeSlots = form.querySelector('#bulkTimeSlots') || form.querySelector('[name=timeSlots]');
        const errBox = form.querySelector('#bulkDateClientError');
        const today = form.closest('[data-today]')?.getAttribute('data-today') || todayIso();

        function syncMin() {
            if (startInput) startInput.min = today;
            if (endInput) {
                endInput.min = startInput && startInput.value ? startInput.value : today;
            }
        }

        function showClientError(msg) {
            if (!errBox) return;
            if (msg) {
                errBox.textContent = msg;
                errBox.classList.remove('hidden');
            } else {
                errBox.textContent = '';
                errBox.classList.add('hidden');
            }
        }

        function runCheck() {
            syncMin();
            if (!startInput || !endInput) return { ok: true };
            const v = validateBulkTimeSlots(startInput.value, endInput.value, timeSlots ? timeSlots.value : '');
            showClientError(v.ok ? '' : v.message);
            return v;
        }

        if (startInput) {
            startInput.addEventListener('change', function () {
                if (endInput && endInput.value && endInput.value < startInput.value) {
                    endInput.value = startInput.value;
                }
                syncMin();
                runCheck();
            });
        }
        if (endInput) endInput.addEventListener('change', runCheck);
        if (timeSlots) timeSlots.addEventListener('change', runCheck);
        syncMin();
        runCheck();

        form.addEventListener('submit', function (e) {
            const v = runCheck();
            if (!v.ok) {
                e.preventDefault();
                showClientError(v.message);
            }
        });
    }

    function bindFilterDateValidation(form) {
        const fromInput = form.querySelector('#filterFromDate') || form.querySelector('[name=fromDate]');
        const toInput = form.querySelector('#filterToDate') || form.querySelector('[name=toDate]');
        const errBox = document.getElementById('filterDateClientError');

        function showErr(msg) {
            if (!errBox) return;
            if (msg) {
                errBox.textContent = msg;
                errBox.classList.remove('hidden');
            } else {
                errBox.textContent = '';
                errBox.classList.add('hidden');
            }
        }

        function check() {
            if (!fromInput || !toInput || !fromInput.value || !toInput.value) {
                showErr('');
                return { ok: true };
            }
            const v = validateDateRange(fromInput.value, toInput.value, false);
            showErr(v.ok ? '' : v.message);
            if (toInput && fromInput.value) {
                toInput.min = fromInput.value;
            }
            return v;
        }

        if (fromInput) fromInput.addEventListener('change', check);
        if (toInput) toInput.addEventListener('change', check);
        form.addEventListener('submit', function (e) {
            const v = check();
            if (!v.ok) e.preventDefault();
        });
    }

    const fmt = new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });

    function parseLocalDateTime(value) {
        if (!value) return null;
        const d = new Date(value);
        return isNaN(d.getTime()) ? null : d;
    }

    function formatDt(d) {
        return d ? fmt.format(d) : '';
    }

    async function checkConflict(roomId, startValue, durationMinutes, excludeId) {
        const params = new URLSearchParams();
        params.set('roomId', String(roomId));
        params.set('start', startValue);
        params.set('durationMinutes', String(durationMinutes || 120));
        if (excludeId) params.set('excludeId', String(excludeId));
        const res = await fetch('/admin/showtimes/check-conflict?' + params.toString(), {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin'
        });
        if (!res.ok) {
            throw new Error('HTTP ' + res.status);
        }
        return res.json();
    }

    function ensureBox(form) {
        let box = form.querySelector('.showtime-conflict-alert');
        if (!box) {
            box = document.createElement('div');
            box.className = 'showtime-conflict-alert hidden mb-4 p-3 rounded text-sm';
            form.insertBefore(box, form.firstChild);
        }
        return box;
    }

    function showAlert(box, ok, message) {
        box.classList.remove('hidden', 'bg-red-100', 'text-red-800', 'bg-green-50', 'text-green-800', 'border', 'border-red-200', 'border-green-200');
        if (ok) {
            box.classList.add('bg-green-50', 'text-green-800', 'border', 'border-green-200');
        } else {
            box.classList.add('bg-red-100', 'text-red-800', 'border', 'border-red-200');
        }
        box.textContent = message;
    }

    function bindForm(form, options) {
        const roomSelect = form.querySelector('[name=roomId]');
        const startInput = form.querySelector('[name=startTime]');
        const movieSelect = form.querySelector('#movieSelect');
        const tmdbField = form.querySelector('#tmdbIdField');
        if (!roomSelect || !startInput) return;

        let timer = null;

        async function runCheck() {
            const roomId = roomSelect.value;
            const startValue = startInput.value;
            if (!roomId || !startValue) return;

            let duration = options.defaultDuration || 120;
            if (movieSelect && movieSelect.selectedIndex > 0) {
                const opt = movieSelect.options[movieSelect.selectedIndex];
                const d = parseInt(opt.getAttribute('data-duration') || '', 10);
                if (!isNaN(d) && d > 0) duration = d;
            }

            const box = ensureBox(form);
            box.textContent = 'Đang kiểm tra lịch phòng…';
            box.classList.remove('hidden');
            box.className = 'showtime-conflict-alert mb-4 p-3 rounded text-sm bg-gray-50 text-gray-600 border';

            try {
                const data = await checkConflict(roomId, startValue, duration, options.excludeShowtimeId);
                showAlert(box, !data.conflict, data.message || (data.conflict ? 'Trùng lịch' : 'Hợp lệ'));
                form.dataset.scheduleConflict = data.conflict ? 'true' : 'false';
            } catch (e) {
                console.error(e);
                showAlert(box, false, 'Không kiểm tra được lịch phòng. Thử lại sau khi chọn đủ phòng và giờ.');
                form.dataset.scheduleConflict = 'unknown';
            }
        }

        function scheduleCheck() {
            if (timer) clearTimeout(timer);
            timer = setTimeout(runCheck, 350);
        }

        roomSelect.addEventListener('change', scheduleCheck);
        startInput.addEventListener('change', scheduleCheck);
        if (movieSelect) movieSelect.addEventListener('change', scheduleCheck);

        form.addEventListener('submit', function (e) {
            if (form.dataset.scheduleConflict === 'true') {
                e.preventDefault();
                const box = ensureBox(form);
                showAlert(box, false, box.textContent || 'Khung giờ trùng suất khác trong cùng phòng. Chọn giờ gợi ý hoặc phòng khác.');
            }
        });
    }

    document.querySelectorAll('form[data-showtime-schedule-check]').forEach(function (form) {
        bindForm(form, {
            defaultDuration: parseInt(form.dataset.defaultDuration || '120', 10),
            excludeShowtimeId: form.dataset.excludeShowtimeId || null
        });
    });

    document.querySelectorAll('form[data-bulk-date-validation]').forEach(bindBulkDateValidation);
    document.querySelectorAll('form[data-filter-date-validation]').forEach(bindFilterDateValidation);

    const startTimeInput = document.getElementById('showtimeStartTime');
    if (startTimeInput) {
        startTimeInput.min = nowIsoLocal();
        startTimeInput.addEventListener('change', function () {
            if (startTimeInput.value && startTimeInput.value < nowIsoLocal()) {
                startTimeInput.setCustomValidity('Giờ bắt đầu phải sau thời điểm hiện tại.');
            } else {
                startTimeInput.setCustomValidity('');
            }
        });
    }

    /** Bulk: kiểm tra mẫu ngày đầu + khung giờ đầu */
    const bulkForm = document.querySelector('form[action*="/admin/showtimes/bulk"]');
    if (bulkForm) {
        const roomSelect = bulkForm.querySelector('[name=roomId]');
        const startDate = bulkForm.querySelector('[name=startDate]');
        const timeSlots = bulkForm.querySelector('[name=timeSlots]');
        const movieSelect = bulkForm.querySelector('#movieSelect');
        const box = ensureBox(bulkForm);
        bulkForm.addEventListener('submit', function (e) {
            if (e.defaultPrevented) return;
            box.classList.add('hidden');
        });
        async function previewBulk() {
            if (!roomSelect || !startDate || !timeSlots || !startDate.value) return;
            const dateCheck = validateBulkTimeSlots(
                startDate.value,
                bulkForm.querySelector('[name=endDate]')?.value || startDate.value,
                timeSlots.value
            );
            if (!dateCheck.ok) {
                showAlert(box, false, dateCheck.message);
                return;
            }
            const firstSlot = (timeSlots.value || '10:00').split(/[,;\s]+/).filter(Boolean)[0] || '10:00';
            const startVal = startDate.value + 'T' + (firstSlot.length === 5 ? firstSlot + ':00' : firstSlot);
            let duration = 120;
            if (movieSelect && movieSelect.selectedIndex > 0) {
                const d = parseInt(movieSelect.options[movieSelect.selectedIndex].getAttribute('data-duration') || '', 10);
                if (!isNaN(d) && d > 0) duration = d;
            }
            try {
                const data = await checkConflict(roomSelect.value, startVal, duration, null);
                showAlert(box, !data.conflict,
                    'Xem trước khung đầu: ' + (data.message || ''));
            } catch (e) {
                console.error(e);
            }
        }
        [roomSelect, startDate, timeSlots, movieSelect].forEach(function (el) {
            if (el) el.addEventListener('change', function () {
                setTimeout(previewBulk, 300);
            });
        });
    }
})();
