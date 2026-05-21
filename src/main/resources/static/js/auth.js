(function () {
    'use strict';

    initPasswordToggles();
    initSubmitLoading();
    initPasswordStrength();
    initPasswordMatch();
    initRegisterFormGuard();
})();

function initRegisterFormGuard() {
    const form = document.getElementById('registerForm');
    if (!form) return;
    form.addEventListener('submit', function (event) {
        if (!form.checkValidity()) {
            event.preventDefault();
            form.reportValidity();
        }
    });
}

function initPasswordToggles() {
    document.querySelectorAll('[data-password-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const wrap = btn.closest('.auth-field__control');
            if (!wrap) return;
            const input = wrap.querySelector('input');
            const icon = btn.querySelector('i');
            if (!input) return;
            const show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            if (icon) {
                icon.classList.toggle('fa-eye', !show);
                icon.classList.toggle('fa-eye-slash', show);
            }
        });
    });
}

function initSubmitLoading() {
    document.querySelectorAll('form.auth-form').forEach(function (form) {
        form.addEventListener('submit', function () {
            const btn = form.querySelector('[data-auth-submit]');
            if (!btn || btn.disabled) return;
            const loading = btn.getAttribute('data-loading') || 'Đang xử lý…';
            const label = btn.querySelector('span');
            if (label) {
                btn.dataset.originalText = label.textContent;
                label.textContent = loading;
            }
            btn.disabled = true;
        });
    });
}

function initPasswordStrength() {
    const input = document.querySelector('[data-password-strength]');
    const panel = document.getElementById('passwordStrength');
    if (!input || !panel) return;

    const fill = panel.querySelector('[data-strength-fill]');
    const text = panel.querySelector('[data-strength-text]');
    const labels = {
        weak: panel.getAttribute('data-label-weak') || 'Yếu',
        fair: panel.getAttribute('data-label-fair') || 'Trung bình',
        good: panel.getAttribute('data-label-good') || 'Khá',
        strong: panel.getAttribute('data-label-strong') || 'Mạnh'
    };

    function evaluate(password) {
        const rules = {
            length: password.length >= 8,
            lower: /[a-z]/.test(password),
            upper: /[A-Z]/.test(password),
            digit: /[0-9]/.test(password),
            special: /[^A-Za-z0-9]/.test(password)
        };
        let score = 0;
        if (password.length >= 6) score += 10;
        if (rules.length) score += 20;
        if (rules.lower) score += 15;
        if (rules.upper) score += 15;
        if (rules.digit) score += 15;
        if (rules.special) score += 15;
        if (password.length >= 12) score += 10;

        let level = 'weak';
        if (score >= 85 && rules.length && rules.lower && rules.upper && rules.digit) {
            level = 'strong';
        } else if (score >= 65) {
            level = 'good';
        } else if (score >= 40) {
            level = 'fair';
        }

        return { rules, level, score };
    }

    function updateRules(rules) {
        panel.querySelectorAll('[data-rule]').forEach(function (li) {
            const key = li.getAttribute('data-rule');
            const met = !!rules[key];
            li.classList.toggle('is-met', met);
            const icon = li.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-circle', !met);
                icon.classList.toggle('fa-circle-check', met);
                icon.classList.toggle('fa-regular', !met);
                icon.classList.toggle('fa-solid', met);
            }
        });
    }

    function render() {
        const value = input.value || '';
        if (!value) {
            panel.hidden = true;
            panel.removeAttribute('data-level');
            if (text) text.textContent = '—';
            if (fill) fill.style.width = '0%';
            return;
        }

        panel.hidden = false;
        const result = evaluate(value);
        panel.setAttribute('data-level', result.level);
        if (text) text.textContent = labels[result.level] || result.level;
        updateRules(result.rules);

        const minForSubmit = result.rules.length && result.rules.lower
            && result.rules.upper && result.rules.digit;
        input.setCustomValidity(minForSubmit ? '' : 'Password too weak');
    }

    input.addEventListener('input', render);
    input.addEventListener('blur', render);
    render();
}

function initPasswordMatch() {
    const confirm = document.getElementById('confirmPassword');
    const password = document.getElementById('regPassword');
    const hint = document.getElementById('passwordMatchHint');
    if (!confirm || !password || !hint) return;

    const okText = hint.getAttribute('data-match-ok') || 'Mật khẩu khớp';
    const badText = hint.getAttribute('data-match-bad') || 'Mật khẩu chưa khớp';

    function check() {
        const pv = password.value;
        const cv = confirm.value;
        if (!cv) {
            hint.hidden = true;
            confirm.setCustomValidity('');
            return;
        }
        hint.hidden = false;
        const match = pv === cv;
        hint.textContent = match ? okText : badText;
        hint.classList.toggle('is-ok', match);
        hint.classList.toggle('is-bad', !match);
        confirm.setCustomValidity(match ? '' : 'Passwords do not match');
    }

    password.addEventListener('input', check);
    confirm.addEventListener('input', check);
}
