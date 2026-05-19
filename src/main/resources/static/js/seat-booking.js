(function () {
    const countdownEl = document.getElementById('seatLockCountdown');
    const body = document.body;
    if (!countdownEl || !body) {
        return;
    }

    const minutes = parseInt(body.getAttribute('data-seat-lock-minutes') || '15', 10);
    let remaining = Math.max(1, minutes) * 60;

    function tick() {
        const m = Math.floor(remaining / 60);
        const s = remaining % 60;
        countdownEl.textContent = String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
        if (remaining <= 0) {
            return;
        }
        remaining -= 1;
        setTimeout(tick, 1000);
    }

    tick();
})();
