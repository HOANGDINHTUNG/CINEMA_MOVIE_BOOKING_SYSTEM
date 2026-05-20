(function () {
    'use strict';

    function bodyMsg(name) {
        var body = document.body;
        return body ? body.getAttribute(name) || '' : '';
    }

    function initLockCountdown() {
        var countdownEl = document.getElementById('seatLockCountdown');
        var timerWrap = document.getElementById('seatLockTimerWrap');
        var body = document.body;
        if (!countdownEl || !body) return;

        var expiresRaw = body.getAttribute('data-lock-expires-at');
        var remaining;
        if (expiresRaw) {
            var expiresMs = new Date(expiresRaw).getTime();
            remaining = Math.floor((expiresMs - Date.now()) / 1000);
        } else {
            var minutes = parseInt(body.getAttribute('data-seat-lock-minutes') || '15', 10);
            remaining = Math.max(1, minutes) * 60;
        }

        if (remaining <= 0 && expiresRaw) {
            countdownEl.textContent = '00:00';
            if (window.UiToast) {
                UiToast.info(bodyMsg('data-msg-lock-expired') || 'Thời gian giữ ghế đã hết. Vui lòng chọn lại.');
            }
            return;
        }

        function tick() {
            if (remaining <= 0) {
                countdownEl.textContent = '00:00';
                if (timerWrap) timerWrap.classList.add('is-urgent');
                return;
            }
            var m = Math.floor(remaining / 60);
            var s = remaining % 60;
            countdownEl.textContent = String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
            if (remaining <= 120 && timerWrap) {
                timerWrap.classList.add('is-urgent');
            }
            remaining -= 1;
            setTimeout(tick, 1000);
        }

        tick();
    }

    document.addEventListener('DOMContentLoaded', initLockCountdown);
})();
