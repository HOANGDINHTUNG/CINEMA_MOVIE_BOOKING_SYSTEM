(function () {
    const tabs = document.querySelectorAll('.schedule-date-tab');
    const panels = document.querySelectorAll('.schedule-time-panel');
    if (!tabs.length || !panels.length) {
        return;
    }

    function activateDay(index) {
        tabs.forEach(function (tab) {
            const active = tab.getAttribute('data-day-index') === String(index);
            tab.classList.toggle('active', active);
            tab.setAttribute('aria-selected', active ? 'true' : 'false');
        });
        panels.forEach(function (panel) {
            panel.classList.toggle('active', panel.getAttribute('data-day-index') === String(index));
        });
    }

    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            activateDay(tab.getAttribute('data-day-index'));
        });
    });
})();
