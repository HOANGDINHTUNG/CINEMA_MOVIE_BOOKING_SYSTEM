(function () {
    var toggle = document.getElementById('scheduleFiltersToggle');
    var sidebar = document.getElementById('scheduleSidebar');
    var backdrop = document.getElementById('scheduleSidebarBackdrop');
    if (!toggle || !sidebar) {
        return;
    }

    function setOpen(open) {
        sidebar.classList.toggle('is-open', open);
        if (backdrop) {
            backdrop.hidden = !open;
        }
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        document.body.classList.toggle('schedule-filters-open', open);
    }

    toggle.addEventListener('click', function () {
        setOpen(!sidebar.classList.contains('is-open'));
    });

    if (backdrop) {
        backdrop.addEventListener('click', function () {
            setOpen(false);
        });
    }

    window.addEventListener('resize', function () {
        if (window.innerWidth > 991) {
            setOpen(false);
        }
    });
})();
