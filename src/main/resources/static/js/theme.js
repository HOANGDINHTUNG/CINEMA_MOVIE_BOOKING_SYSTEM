(function () {
    var STORAGE_KEY = 'cinema_theme';

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem(STORAGE_KEY, theme);
        document.querySelectorAll('[data-theme-btn]').forEach(function (btn) {
            btn.classList.toggle('active', btn.getAttribute('data-theme-btn') === theme);
        });
    }

    var saved = localStorage.getItem(STORAGE_KEY) || 'light';
    applyTheme(saved);

    document.addEventListener('click', function (e) {
        var btn = e.target.closest('[data-theme-btn]');
        if (!btn) return;
        e.preventDefault();
        applyTheme(btn.getAttribute('data-theme-btn'));
    });
})();
