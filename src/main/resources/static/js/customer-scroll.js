/**
 * Customer site: glass header on scroll + gentle wheel momentum.
 */
(function () {
    var body = document.body;
    if (!body || !body.classList.contains('customer-site')) {
        return;
    }

    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    function initHeaderScrollState() {
        var threshold = 20;

        function update() {
            body.classList.toggle('header-scrolled', window.scrollY > threshold);
        }

        update();
        window.addEventListener('scroll', update, { passive: true });
    }

    function canScrollInside(target, deltaY) {
        var node = target;
        while (node && node !== document.body && node !== document.documentElement) {
            var style = window.getComputedStyle(node);
            var overflowY = style.overflowY;
            var scrollable = (overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay')
                && node.scrollHeight > node.clientHeight + 1;
            if (scrollable) {
                if (deltaY > 0 && node.scrollTop + node.clientHeight < node.scrollHeight - 1) {
                    return true;
                }
                if (deltaY < 0 && node.scrollTop > 0) {
                    return true;
                }
            }
            node = node.parentElement;
        }
        return false;
    }

    function initSmoothWheelScroll() {
        if (reduceMotion) {
            return;
        }

        document.documentElement.classList.add('customer-smooth-scroll');

        var targetY = window.scrollY;
        var currentY = window.scrollY;
        var rafId = 0;
        var friction = 0.11;
        var wheelGain = 0.88;

        function maxScroll() {
            return Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
        }

        function clampScroll(y) {
            return Math.max(0, Math.min(maxScroll(), y));
        }

        function tick() {
            var diff = targetY - currentY;
            if (Math.abs(diff) < 0.6) {
                currentY = targetY;
                window.scrollTo(0, currentY);
                rafId = 0;
                return;
            }
            currentY += diff * friction;
            window.scrollTo(0, currentY);
            rafId = window.requestAnimationFrame(tick);
        }

        function schedule() {
            if (!rafId) {
                rafId = window.requestAnimationFrame(tick);
            }
        }

        window.addEventListener(
            'wheel',
            function (e) {
                if (e.ctrlKey || e.metaKey || e.shiftKey) {
                    return;
                }
                if (canScrollInside(e.target, e.deltaY)) {
                    return;
                }
                e.preventDefault();
                if (!rafId) {
                    currentY = window.scrollY;
                }
                targetY = clampScroll(targetY + e.deltaY * wheelGain);
                schedule();
            },
            { passive: false }
        );

        window.addEventListener(
            'scroll',
            function () {
                if (!rafId) {
                    targetY = window.scrollY;
                    currentY = window.scrollY;
                }
            },
            { passive: true }
        );

        window.addEventListener('resize', function () {
            targetY = clampScroll(targetY);
            if (!rafId) {
                currentY = clampScroll(currentY);
            }
        }, { passive: true });
    }

    initHeaderScrollState();
    initSmoothWheelScroll();
})();
