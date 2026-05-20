(function (global) {
    var container;

    function ensureContainer() {
        if (container && document.body.contains(container)) {
            return container;
        }
        container = document.createElement('div');
        container.className = 'ui-toast-container';
        container.setAttribute('aria-live', 'polite');
        container.setAttribute('aria-atomic', 'true');
        document.body.appendChild(container);
        return container;
    }

    function showToast(message, type, durationMs) {
        if (!message) return;
        var root = ensureContainer();
        var toast = document.createElement('div');
        toast.className = 'ui-toast ui-toast--' + (type || 'info');
        toast.textContent = message;
        root.appendChild(toast);
        var ms = durationMs == null ? 4000 : durationMs;
        setTimeout(function () {
            toast.style.opacity = '0';
            toast.style.transition = 'opacity 200ms';
            setTimeout(function () {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 220);
        }, ms);
    }

    global.UiToast = {
        success: function (msg) { showToast(msg, 'success'); },
        error: function (msg) { showToast(msg, 'error'); },
        info: function (msg) { showToast(msg, 'info'); }
    };
})(window);
