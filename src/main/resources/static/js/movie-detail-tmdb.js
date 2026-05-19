(function () {
    function initCastExpand() {
        var section = document.querySelector("[data-cast-section]");
        if (!section) return;

        var step = parseInt(section.getAttribute("data-cast-step") || "20", 10) || 20;
        var cards = Array.prototype.slice.call(section.querySelectorAll("[data-cast-card]"));
        var countLabel = section.querySelector("[data-cast-count-label]");
        var moreBtn = section.querySelector("[data-cast-more]");
        var countTpl = section.getAttribute("data-cast-count-template") || "";
        var moreTpl = section.getAttribute("data-cast-more-template") || "";

        var total = cards.length;
        var visible = Math.min(step, total);

        function applyTpl(tpl, map) {
            var s = tpl;
            Object.keys(map).forEach(function (k) {
                s = s.split(k).join(String(map[k]));
            });
            return s;
        }

        function sync() {
            for (var i = 0; i < cards.length; i++) {
                if (i < visible) {
                    cards[i].classList.remove("is-collapsed");
                } else {
                    cards[i].classList.add("is-collapsed");
                }
            }
            if (countLabel && countTpl) {
                countLabel.textContent = applyTpl(countTpl, { __A__: Math.min(visible, total), __B__: total });
            }
            if (moreBtn) {
                var remain = total - visible;
                if (remain <= 0) {
                    moreBtn.hidden = true;
                } else {
                    moreBtn.hidden = false;
                    var load = Math.min(step, remain);
                    moreBtn.textContent = moreTpl ? applyTpl(moreTpl, { __N__: load }) : "";
                }
            }
        }

        if (moreBtn) {
            moreBtn.addEventListener("click", function () {
                visible = Math.min(total, visible + step);
                sync();
            });
        }
        sync();
    }

    function initVideoModal() {
        var modal = document.getElementById("movieVideoModal");
        if (!modal) return;

        var iframe = document.getElementById("movieVideoModalIframe");
        var titleEl = document.getElementById("movieVideoModalTitle");
        var ytLink = document.getElementById("movieVideoModalYoutube");
        var lastFocus = null;

        function openModal(key, title, watchUrl) {
            if (!key || !iframe) return;
            lastFocus = document.activeElement;
            iframe.src = "https://www.youtube.com/embed/" + encodeURIComponent(key) + "?autoplay=1&rel=0";
            iframe.title = title || "Video";
            if (titleEl) {
                titleEl.textContent = title || "";
            }
            if (ytLink) {
                if (watchUrl) {
                    ytLink.href = watchUrl;
                    ytLink.hidden = false;
                } else {
                    ytLink.href = "https://www.youtube.com/watch?v=" + encodeURIComponent(key);
                    ytLink.hidden = false;
                }
            }
            modal.hidden = false;
            modal.classList.add("is-open");
            modal.setAttribute("aria-hidden", "false");
            document.body.classList.add("movie-video-modal-open");
        }

        function closeModal() {
            modal.hidden = true;
            modal.classList.remove("is-open");
            modal.setAttribute("aria-hidden", "true");
            document.body.classList.remove("movie-video-modal-open");
            if (iframe) {
                iframe.src = "";
            }
            if (lastFocus && typeof lastFocus.focus === "function") {
                lastFocus.focus();
            }
        }

        document.querySelectorAll("[data-video-play]").forEach(function (btn) {
            btn.addEventListener("click", function () {
                openModal(
                    btn.getAttribute("data-youtube-key"),
                    btn.getAttribute("data-video-title"),
                    btn.getAttribute("data-watch-url")
                );
            });
        });

        modal.querySelectorAll("[data-video-modal-close]").forEach(function (el) {
            el.addEventListener("click", closeModal);
        });

        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && modal.classList.contains("is-open")) {
                closeModal();
            }
        });
    }

    function initMovieCarousels() {
        var carousels = document.querySelectorAll(".tmdb-movie-carousel");
        carousels.forEach(function (section) {
            var track = section.querySelector("[data-row-scroll]");
            if (!track) return;
            var delta = function () {
                return Math.max(280, Math.floor(track.clientWidth * 0.85));
            };
            section.querySelectorAll(".tmdb-row-nav__btn[data-scroll]").forEach(function (btn) {
                btn.addEventListener("click", function () {
                    var dir = btn.getAttribute("data-scroll") === "prev" ? -1 : 1;
                    track.scrollBy({ left: dir * delta(), behavior: "smooth" });
                });
            });
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            initCastExpand();
            initVideoModal();
            initMovieCarousels();
        });
    } else {
        initCastExpand();
        initVideoModal();
        initMovieCarousels();
    }
})();
