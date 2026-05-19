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
    const titleEl = document.getElementById('seatSummaryTitle');
    const detailEl = document.getElementById('seatSummaryDetail');
    const totalEl = document.getElementById('seatSummaryTotal');
    const submitBtn = document.getElementById('seatSubmitBtn');

    function seatUnitPrice(type) {
        if (type && type.toUpperCase() === 'VIP') {
            return basePrice * vipMultiplier;
        }
        return basePrice;
    }

    function updateSummary() {
        const checked = form.querySelectorAll('input[name="seatIds"]:checked');
        if (!titleEl) return;

        if (checked.length === 0) {
            titleEl.textContent = bodyMsg('data-label-no-seat') || 'Chưa chọn ghế';
            if (detailEl) detailEl.textContent = '';
            if (totalEl) totalEl.textContent = '';
            if (submitBtn) submitBtn.disabled = true;
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
    }

    form.querySelectorAll('input[name="seatIds"]').forEach(function (input) {
        input.addEventListener('change', function () {
            const checked = form.querySelectorAll('input[name="seatIds"]:checked');
            if (input.checked && checked.length > maxSeats) {
                input.checked = false;
                alert(applyMsgTpl(bodyMsg('data-msg-seat-max'), { __0__: maxSeats }) || maxSeats);
                return;
            }
            const seat = input.closest('.seat');
            if (seat) {
                seat.classList.toggle('selected', input.checked);
            }
            updateSummary();
        });
    });

    form.addEventListener('submit', function (event) {
        const checked = form.querySelectorAll('input[name="seatIds"]:checked');
        if (checked.length === 0) {
            event.preventDefault();
            alert(bodyMsg('data-msg-seat-pick-at-least') || 'Vui lòng chọn ít nhất một ghế.');
            return;
        }
        if (checked.length > maxSeats) {
            event.preventDefault();
            alert('Mỗi đơn chỉ được chọn tối đa ' + maxSeats + ' ghế.');
            return;
        }
        var confirmTpl = bodyMsg('data-msg-seat-confirm');
        if (!confirm(confirmTpl ? applyMsgTpl(confirmTpl, { __0__: checked.length }) : checked.length)) {
            event.preventDefault();
        }
    });

    updateSummary();
}

/* ---------- Checkout ---------- */
function initCheckoutForm() {
    const form = document.getElementById('checkoutForm');
    if (!form) return;

    const baseTotal = parseFloat(form.dataset.estimatedTotal || '0');
    const totalDisplay = document.getElementById('checkoutLiveTotal');

    function updateCheckoutTotal() {
        let comboTotal = 0;
        form.querySelectorAll('input[name^="combo_"]').forEach(function (input) {
            const price = parseFloat(input.dataset.price || '0');
            const qty = parseInt(input.value, 10) || 0;
            comboTotal += price * qty;
        });
        const grand = baseTotal + comboTotal;
        if (totalDisplay) {
            totalDisplay.textContent = formatVnd(grand);
        }
    }

    form.querySelectorAll('input[name^="combo_"]').forEach(function (input) {
        input.addEventListener('input', updateCheckoutTotal);
        input.addEventListener('change', updateCheckoutTotal);
    });

    form.addEventListener('submit', function (event) {
        const mode = form.querySelector('input[name="paymentMode"]:checked');
        const modeText = mode && mode.value === 'COUNTER'
            ? bodyMsg('data-msg-checkout-counter')
            : bodyMsg('data-msg-checkout-online');
        const confirmTpl = bodyMsg('data-msg-checkout-confirm');
        if (!confirm(confirmTpl ? applyMsgTpl(confirmTpl, { __0__: modeText }) : modeText)) {
            event.preventDefault();
        }
    });

    updateCheckoutTotal();
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
                selector: '#checkoutLiveTotal',
                title: 'Tổng tiền',
                text: 'Tổng dự kiến gồm vé và combo (nếu có). Kiểm tra kỹ trước khi xác nhận.'
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
    initOnboarding();
    initResetGuideButton();
});
