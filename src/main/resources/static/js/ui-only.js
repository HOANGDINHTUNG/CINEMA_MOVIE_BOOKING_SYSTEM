function toggleMenu() {
    const nav = document.querySelector('.nav-menu');
    if (nav) {
        nav.classList.toggle('open');
    }
}

function formatVnd(amount) {
    if (amount == null || isNaN(amount)) return '';
    var lang = (document.documentElement.lang || 'vi').toLowerCase();
    var locale = lang.startsWith('en') ? 'en-US' : 'vi-VN';
    return new Intl.NumberFormat(locale).format(Math.round(amount)) + ' đ';
}

function applyMsgTpl(tpl, map) {
    if (!tpl) return '';
    var s = tpl;
    Object.keys(map).forEach(function (k) {
        s = s.split(k).join(String(map[k]));
    });
    return s;
}

function bodyMsg(name) {
    var body = document.body;
    return body ? body.getAttribute(name) || '' : '';
}

function hidePageLoader() {
    const loader = document.getElementById('pageLoader');
    if (!loader || loader.dataset.hidden === 'true') {
        return;
    }
    loader.dataset.hidden = 'true';
    loader.style.opacity = '0';
    loader.style.pointerEvents = 'none';
    setTimeout(function () {
        loader.style.display = 'none';
    }, 280);
}

/* Ẩn loader khi DOM sẵn sàng — không chờ tải hết ảnh TMDB (window.load có thể rất lâu). */
document.addEventListener('DOMContentLoaded', function () {
    hidePageLoader();
});

/* Dự phòng nếu DOMContentLoaded đã chạy trước khi script load */
if (document.readyState !== 'loading') {
    hidePageLoader();
}

/* Tối đa 2.5s vẫn tắt loader dù có sự cố */
setTimeout(hidePageLoader, 2500);

/* ---------- Seat selection ---------- */
function initSeatSelection() {
    const form = document.getElementById('seatForm');
    if (!form) return;

    const basePrice = parseFloat(form.dataset.basePrice || '0');
    const vipMultiplier = parseFloat(form.dataset.vipMultiplier || '1.5');
    const maxSeats = parseInt(form.dataset.maxSeats || '8', 10);
    const syncEnabled = form.dataset.syncEnabled === 'true';
    const serverLocked = form.dataset.seatsLockedOnServer === 'true';
    const syncUrl = form.dataset.syncUrl || '';
    const releaseUrl = form.dataset.releaseUrl || '';
    const titleEl = document.getElementById('seatSummaryTitle');
    const detailEl = document.getElementById('seatSummaryDetail');
    const totalEl = document.getElementById('seatSummaryTotal');
    const submitBtn = document.getElementById('seatSubmitBtn');
    const clearAllBtn = document.getElementById('seatClearAllBtn');
    const backLink = document.getElementById('seatBackLink');
    const chipsWrap = document.getElementById('seatChipsWrap');
    const modal = document.getElementById('seatConfirmModal');
    let pendingSubmit = false;
    let syncDebounceTimer = null;
    let syncInFlight = false;

    function getCsrfField() {
        return form.querySelector('input[type="hidden"]:not([name="showtimeId"])');
    }

    function collectSeatIds() {
        return Array.from(form.querySelectorAll('input[name="seatIds"]:checked'))
            .map(function (input) { return input.value; });
    }

    function postSeatAction(url, seatIds) {
        const csrf = getCsrfField();
        const fd = new FormData();
        if (seatIds && seatIds.length) {
            seatIds.forEach(function (id) { fd.append('seatIds', id); });
        }
        if (csrf) {
            fd.append(csrf.name, csrf.value);
        }
        return fetch(url, {
            method: 'POST',
            body: fd,
            credentials: 'same-origin',
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        }).then(function (res) {
            return res.json().catch(function () { return {}; }).then(function (data) {
                if (!res.ok || !data.ok) {
                    throw new Error(data.message || bodyMsg('data-msg-seat-sync-failed') || 'Sync failed');
                }
                return data;
            });
        });
    }

    function notifySyncError(message) {
        const msg = message || bodyMsg('data-msg-seat-sync-failed') || 'Không cập nhật được ghế.';
        if (window.UiToast) {
            UiToast.error(msg);
        } else {
            alert(msg);
        }
    }

    function runServerSync() {
        if (!serverLocked || !syncEnabled || !syncUrl || syncInFlight) {
            return Promise.resolve();
        }
        syncInFlight = true;
        return postSeatAction(syncUrl, collectSeatIds())
            .catch(function (err) {
                notifySyncError(err.message);
                window.location.reload();
                throw err;
            })
            .finally(function () {
                syncInFlight = false;
            });
    }

    function scheduleServerSync() {
        if (!serverLocked || !syncEnabled) return;
        clearTimeout(syncDebounceTimer);
        syncDebounceTimer = setTimeout(function () {
            runServerSync();
        }, 400);
    }

    function seatUnitPrice(type) {
        if (type && type.toUpperCase() === 'VIP') {
            return basePrice * vipMultiplier;
        }
        return basePrice;
    }

    function renderChips(checked) {
        if (!chipsWrap) return;
        chipsWrap.innerHTML = '';
        const removeLabel = bodyMsg('data-label-remove-seat') || 'Xóa ghế';
        checked.forEach(function (input) {
            const chip = document.createElement('span');
            const isVip = (input.dataset.seatType || '').toUpperCase() === 'VIP';
            chip.className = 'seat-chip' + (isVip ? ' seat-chip--vip' : '');
            chip.innerHTML = '<span>' + (input.dataset.seatLabel || '') + '</span>' +
                '<button type="button" class="seat-chip__remove" aria-label="' + removeLabel + ' ' +
                (input.dataset.seatLabel || '') + '">&times;</button>';
            chip.querySelector('.seat-chip__remove').addEventListener('click', function () {
                input.checked = false;
                const seat = input.closest('.seat');
                if (seat) seat.classList.remove('selected');
                updateSummary();
                scheduleServerSync();
            });
            chipsWrap.appendChild(chip);
        });
    }

    function updateSummary() {
        const checked = form.querySelectorAll('input[name="seatIds"]:checked');
        if (!titleEl) return;

        if (checked.length === 0) {
            titleEl.textContent = bodyMsg('data-label-no-seat') || 'Chưa chọn ghế';
            if (detailEl) detailEl.textContent = '';
            if (totalEl) totalEl.textContent = '';
            if (submitBtn) submitBtn.disabled = true;
            if (clearAllBtn) clearAllBtn.hidden = true;
            renderChips(checked);
            return;
        }

        let total = 0;
        const labels = [];
        checked.forEach(function (input) {
            const type = input.dataset.seatType || 'NORMAL';
            const label = input.dataset.seatLabel || '';
            total += seatUnitPrice(type);
            if (label) labels.push(label);
        });

        var progressTpl = bodyMsg('data-label-selected-progress');
        titleEl.textContent = progressTpl
            ? applyMsgTpl(progressTpl, { __0__: checked.length, __1__: maxSeats })
            : checked.length + '/' + maxSeats;
        if (detailEl) detailEl.textContent = labels.join(', ');
        var totalTpl = bodyMsg('data-label-estimated-total');
        if (totalEl) {
            totalEl.textContent = totalTpl
                ? applyMsgTpl(totalTpl, { __0__: formatVnd(total) })
                : formatVnd(total);
        }
        if (submitBtn) submitBtn.disabled = false;
        if (clearAllBtn) clearAllBtn.hidden = false;
        renderChips(checked);
    }

    function clearAllSeats() {
        form.querySelectorAll('input[name="seatIds"]:checked').forEach(function (input) {
            input.checked = false;
            const seat = input.closest('.seat');
            if (seat) seat.classList.remove('selected');
        });
        updateSummary();
        scheduleServerSync();
    }

    form.querySelectorAll('input[name="seatIds"]').forEach(function (input) {
        const seat = input.closest('.seat');
        if (seat && input.checked) {
            seat.classList.add('selected');
        }
        input.addEventListener('click', function () {
            input._prevChecked = input.checked;
        });
        input.addEventListener('change', function () {
            const checked = form.querySelectorAll('input[name="seatIds"]:checked');
            if (input.checked && checked.length > maxSeats) {
                input.checked = false;
                if (seat) {
                    seat.classList.remove('selected');
                    seat.classList.add('shake-limit');
                    setTimeout(function () { seat.classList.remove('shake-limit'); }, 400);
                }
                if (window.UiToast) {
                    UiToast.error(applyMsgTpl(bodyMsg('data-msg-seat-max'), { __0__: maxSeats }));
                } else {
                    alert(applyMsgTpl(bodyMsg('data-msg-seat-max'), { __0__: maxSeats }));
                }
                return;
            }
            if (seat) {
                seat.classList.toggle('selected', input.checked);
            }
            updateSummary();
            scheduleServerSync();
        });
    });

    if (clearAllBtn) {
        clearAllBtn.addEventListener('click', clearAllSeats);
    }

    if (backLink && serverLocked && syncEnabled && releaseUrl) {
        backLink.addEventListener('click', function (event) {
            event.preventDefault();
            const msg = bodyMsg('data-msg-seat-back-release')
                || 'Ghế đang được giữ. Bỏ giữ và quay lại?';
            if (!confirm(msg)) return;
            const href = backLink.getAttribute('href');
            postSeatAction(releaseUrl, [])
                .then(function () { window.location.href = href; })
                .catch(function (err) { notifySyncError(err.message); });
        });
    }

    function showConfirmModal(count, onOk) {
        if (!modal) {
            var confirmTpl = bodyMsg('data-msg-seat-confirm');
            if (confirm(confirmTpl ? applyMsgTpl(confirmTpl, { __0__: count }) : true)) onOk();
            return;
        }
        var msgEl = document.getElementById('seatConfirmMessage');
        if (msgEl) {
            var tpl = bodyMsg('data-msg-seat-confirm');
            msgEl.textContent = tpl ? applyMsgTpl(tpl, { __0__: count }) : '';
        }
        modal.hidden = false;
        var okBtn = modal.querySelector('[data-seat-confirm-ok]');
        var cancelBtn = modal.querySelector('[data-seat-confirm-cancel]');
        function close() { modal.hidden = true; }
        if (cancelBtn) cancelBtn.onclick = close;
        modal.onclick = function (e) { if (e.target === modal) close(); };
        if (okBtn) {
            okBtn.onclick = function () {
                close();
                onOk();
            };
        }
    }

    form.addEventListener('submit', function (event) {
        if (pendingSubmit) return;
        const checked = form.querySelectorAll('input[name="seatIds"]:checked');
        if (checked.length === 0) {
            event.preventDefault();
            if (window.UiToast) {
                UiToast.error(bodyMsg('data-msg-seat-pick-at-least') || 'Vui lòng chọn ghế.');
            }
            return;
        }
        if (checked.length > maxSeats) {
            event.preventDefault();
            return;
        }
        event.preventDefault();
        showConfirmModal(checked.length, function () {
            pendingSubmit = true;
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.setAttribute('data-loading-text', submitBtn.textContent);
                submitBtn.textContent = 'Đang xử lý...';
            }
            form.submit();
        });
    });

    updateSummary();
}

/* ---------- Checkout ---------- */
function initCheckoutForm() {
    const form = document.getElementById('checkoutForm');
    if (!form) return;

    const baseTotal = parseFloat(form.dataset.estimatedTotal || '0');
    const paymentModeField = document.getElementById('paymentModeField');
    const subtotalEl = document.getElementById('checkoutCostSubtotal');
    const grandEl = document.getElementById('checkoutGrandTotal');
    const termsCheckbox = document.getElementById('checkoutTermsAgree');
    const submitBtn = form.querySelector('button[type="submit"]');
    const modal = document.getElementById('checkoutConfirmModal');
    let pendingSubmit = false;

    function syncPaymentMode() {
        const provider = form.querySelector('input[name="paymentProvider"]:checked');
        const isCounter = provider && provider.value === 'counter';
        if (paymentModeField) {
            paymentModeField.value = isCounter ? 'COUNTER' : 'ONLINE';
        }
        form.querySelectorAll('.payment-method-card').forEach(function (card) {
            const input = card.querySelector('input[name="paymentProvider"]');
            card.classList.toggle('is-selected', input && input.checked);
        });
    }

    function updateCheckoutTotal() {
        let comboTotal = 0;
        form.querySelectorAll('.checkout-table__combo-row').forEach(function (row) {
            const input = row.querySelector('input[name^="combo_"]');
            const lineEl = row.querySelector('.checkout-combo-line-total');
            if (!input) return;
            const price = parseFloat(input.dataset.price || '0');
            const qty = Math.max(0, parseInt(input.value, 10) || 0);
            const line = price * qty;
            comboTotal += line;
            if (lineEl) {
                lineEl.textContent = formatVnd(line);
            }
        });
        const grand = baseTotal + comboTotal;
        if (subtotalEl) subtotalEl.textContent = formatVnd(grand);
        if (grandEl) grandEl.textContent = formatVnd(grand);
    }

    function bindQtySteppers() {
        form.querySelectorAll('.qty-stepper').forEach(function (wrap) {
            const input = wrap.querySelector('.qty-stepper__input');
            const minus = wrap.querySelector('[data-qty-minus]');
            const plus = wrap.querySelector('[data-qty-plus]');
            if (!input) return;
            function setQty(next) {
                const max = parseInt(input.getAttribute('max'), 10) || 20;
                input.value = String(Math.min(max, Math.max(0, next)));
                updateCheckoutTotal();
            }
            if (minus) minus.addEventListener('click', function () { setQty((parseInt(input.value, 10) || 0) - 1); });
            if (plus) plus.addEventListener('click', function () { setQty((parseInt(input.value, 10) || 0) + 1); });
            input.addEventListener('input', updateCheckoutTotal);
            input.addEventListener('change', updateCheckoutTotal);
        });
    }

    form.querySelectorAll('input[name="paymentProvider"]').forEach(function (radio) {
        radio.addEventListener('change', syncPaymentMode);
    });
    syncPaymentMode();
    bindQtySteppers();

    function showCheckoutConfirm(onOk) {
        const isCounter = paymentModeField && paymentModeField.value === 'COUNTER';
        const modeText = isCounter
            ? bodyMsg('data-msg-checkout-counter')
            : bodyMsg('data-msg-checkout-online');
        const confirmTpl = bodyMsg('data-msg-checkout-confirm');
        const message = confirmTpl ? applyMsgTpl(confirmTpl, { __0__: modeText }) : modeText;

        if (!modal) {
            if (confirm(message)) onOk();
            return;
        }
        var msgEl = document.getElementById('checkoutConfirmMessage');
        if (msgEl) msgEl.textContent = message;
        modal.hidden = false;
        var okBtn = modal.querySelector('[data-checkout-confirm-ok]');
        var cancelBtn = modal.querySelector('[data-checkout-confirm-cancel]');
        function close() { modal.hidden = true; }
        if (cancelBtn) cancelBtn.onclick = close;
        modal.onclick = function (e) { if (e.target === modal) close(); };
        if (okBtn) okBtn.onclick = function () { close(); onOk(); };
    }

    form.addEventListener('submit', function (event) {
        if (pendingSubmit) return;
        event.preventDefault();
        if (termsCheckbox && !termsCheckbox.checked) {
            const msg = bodyMsg('data-msg-checkout-terms') || 'Vui lòng đồng ý điều khoản.';
            if (window.UiToast) UiToast.error(msg);
            else alert(msg);
            return;
        }
        showCheckoutConfirm(function () {
            pendingSubmit = true;
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Đang xử lý...';
            }
            form.submit();
        });
    });

    updateCheckoutTotal();
}

function initCheckoutLockCountdown() {
    var countdownEl = document.getElementById('checkoutLockCountdown');
    var wrap = document.getElementById('checkoutLockTimerWrap');
    if (!countdownEl) return;
    var raw = countdownEl.getAttribute('data-expires-at');
    if (!raw) return;
    var remaining = Math.floor((new Date(raw).getTime() - Date.now()) / 1000);
    if (remaining <= 0) {
        countdownEl.textContent = '00:00';
        if (wrap) wrap.classList.add('is-urgent');
        return;
    }
    function tick() {
        if (remaining <= 0) {
            countdownEl.textContent = '00:00';
            return;
        }
        var m = Math.floor(remaining / 60);
        var s = remaining % 60;
        countdownEl.textContent = String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
        if (remaining <= 120 && wrap) wrap.classList.add('is-urgent');
        remaining -= 1;
        setTimeout(tick, 1000);
    }
    tick();
}

function initFlashToasts() {
    document.querySelectorAll('[data-flash-success]').forEach(function (el) {
        if (window.UiToast && el.textContent.trim()) {
            UiToast.success(el.textContent.trim());
        }
    });
    document.querySelectorAll('[data-flash-error]').forEach(function (el) {
        if (window.UiToast && el.textContent.trim()) {
            UiToast.error(el.textContent.trim());
        }
    });
}

function initCopyTicketCodes() {
    document.querySelectorAll('[data-copy-code]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var code = btn.getAttribute('data-copy-code');
            if (!code) return;
            navigator.clipboard.writeText(code).then(function () {
                if (window.UiToast) {
                    UiToast.success(btn.getAttribute('data-copied-msg') || 'Đã sao chép mã vé');
                }
                btn.textContent = btn.getAttribute('data-copied-msg') || 'Đã sao chép';
                setTimeout(function () {
                    btn.textContent = btn.getAttribute('data-copy-label') || 'Sao chép';
                }, 2000);
            });
        });
    });
}

/* ---------- First-time guide ---------- */
const NCC_GUIDES = {
    home: {
        storageKey: 'ncc_guide_home_v1',
        steps: [
            {
                selector: '.movie-search-bar',
                title: 'Tìm & lọc phim',
                text: 'Gõ tên phim hoặc chọn thể loại để lọc nhanh danh sách phim và suất chiếu bên dưới.'
            },
            {
                selector: '#movieGrid',
                title: 'Chọn phim',
                text: 'Bấm vào poster phim để xem chi tiết và chọn suất chiếu phù hợp.'
            },
            {
                selector: '.quick-nav',
                title: 'Truy cập nhanh',
                text: 'Vào Lịch chiếu, Vé của tôi, Khuyến mãi hoặc Giá vé chỉ với một chạm.'
            }
        ]
    },
    seats: {
        storageKey: 'ncc_guide_seats_v1',
        steps: [
            {
                selector: '#seatGrid',
                title: 'Chọn ghế',
                text: 'Chạm các ghế còn trống (màu tối). Ghế VIP màu cam, ghế đã đặt không chọn được.'
            },
            {
                selector: '#seatCheckoutBar',
                title: 'Xác nhận',
                text: 'Xem tóm tắt ghế và tạm tính ở thanh dưới, rồi bấm Tiếp tục thanh toán.'
            }
        ]
    },
    checkout: {
        storageKey: 'ncc_guide_checkout_v1',
        steps: [
            {
                selector: '.payment-info',
                title: 'Thanh toán',
                text: 'Chọn Online để thanh toán ngay, hoặc Quầy để giữ ghế 15 phút và trả tiền tại rạp.'
            },
            {
                selector: '#checkoutGrandTotal',
                title: 'Tổng tiền',
                text: 'Tổng dự kiến gồm vé và combo (nếu có). Kiểm tra kỹ trước khi thanh toán.'
            }
        ]
    }
};

function initOnboarding() {
    const page = document.body.dataset.guidePage;
    if (!page || !NCC_GUIDES[page]) return;

    const config = NCC_GUIDES[page];
    if (localStorage.getItem(config.storageKey) === 'done') return;

    let stepIndex = 0;
    let overlay = null;
    let spotlight = null;
    let panel = null;

    function cleanupHighlight() {
        document.querySelectorAll('.guide-target-highlight').forEach(function (el) {
            el.classList.remove('guide-target-highlight');
        });
    }

    function closeGuide(markDone) {
        if (markDone) {
            localStorage.setItem(config.storageKey, 'done');
        }
        cleanupHighlight();
        if (overlay && overlay.parentNode) {
            overlay.parentNode.removeChild(overlay);
        }
        overlay = null;
    }

    function showStep(index) {
        const step = config.steps[index];
        const target = document.querySelector(step.selector);
        if (!target) {
            if (index < config.steps.length - 1) {
                showStep(index + 1);
            } else {
                closeGuide(true);
            }
            return;
        }

        cleanupHighlight();
        target.classList.add('guide-target-highlight');
        target.scrollIntoView({ behavior: 'smooth', block: 'center' });

        const rect = target.getBoundingClientRect();
        const pad = 8;
        spotlight.style.top = (rect.top - pad) + 'px';
        spotlight.style.left = (rect.left - pad) + 'px';
        spotlight.style.width = (rect.width + pad * 2) + 'px';
        spotlight.style.height = (rect.height + pad * 2) + 'px';

        panel.querySelector('h3').textContent = step.title;
        panel.querySelector('p').textContent = step.text;
        panel.querySelector('.ncc-guide-step-dots').textContent =
            'Bước ' + (index + 1) + '/' + config.steps.length;

        const nextBtn = panel.querySelector('[data-guide-next]');
        nextBtn.textContent = index === config.steps.length - 1 ? 'Hoàn tất' : 'Tiếp theo';
    }

    function buildOverlay() {
        overlay = document.createElement('div');
        overlay.className = 'ncc-guide-overlay active';

        const backdrop = document.createElement('div');
        backdrop.className = 'ncc-guide-backdrop';
        overlay.appendChild(backdrop);

        spotlight = document.createElement('div');
        spotlight.className = 'ncc-guide-spotlight';
        overlay.appendChild(spotlight);

        panel = document.createElement('div');
        panel.className = 'ncc-guide-panel';
        const h3 = document.createElement('h3');
        const pEl = document.createElement('p');
        const actions = document.createElement('div');
        actions.className = 'ncc-guide-actions';
        const dots = document.createElement('span');
        dots.className = 'ncc-guide-step-dots';
        const skipBtn = document.createElement('button');
        skipBtn.type = 'button';
        skipBtn.className = 'btn btn-sm btn-outline-light';
        skipBtn.setAttribute('data-guide-skip', '');
        skipBtn.textContent = 'Bỏ qua';
        const nextBtnEl = document.createElement('button');
        nextBtnEl.type = 'button';
        nextBtnEl.className = 'btn btn-sm btn-danger';
        nextBtnEl.setAttribute('data-guide-next', '');
        nextBtnEl.textContent = 'Tiếp theo';
        actions.appendChild(dots);
        actions.appendChild(skipBtn);
        actions.appendChild(nextBtnEl);
        panel.appendChild(h3);
        panel.appendChild(pEl);
        panel.appendChild(actions);
        overlay.appendChild(panel);

        document.body.appendChild(overlay);

        spotlight = overlay.querySelector('.ncc-guide-spotlight');
        panel = overlay.querySelector('.ncc-guide-panel');
        overlay.querySelector('[data-guide-skip]').addEventListener('click', function () {
            closeGuide(true);
        });
        overlay.querySelector('[data-guide-next]').addEventListener('click', function () {
            stepIndex++;
            if (stepIndex >= config.steps.length) {
                closeGuide(true);
            } else {
                showStep(stepIndex);
            }
        });
    }

    setTimeout(function () {
        buildOverlay();
        showStep(0);
    }, 600);
}

function initResetGuideButton() {
    const btn = document.getElementById('btnResetGuide');
    if (!btn) return;
    btn.addEventListener('click', function () {
        ['ncc_guide_home_v1', 'ncc_guide_seats_v1', 'ncc_guide_checkout_v1'].forEach(function (k) {
            localStorage.removeItem(k);
        });
        alert('Đã bật lại hướng dẫn. Vào Trang chủ để xem.');
        window.location.href = '/customer/home';
    });
}

document.addEventListener('DOMContentLoaded', function () {
    initSeatSelection();
    initCheckoutForm();
    initCheckoutLockCountdown();
    initFlashToasts();
    initCopyTicketCodes();
    initOnboarding();
    initResetGuideButton();
});
