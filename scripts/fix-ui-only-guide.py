from pathlib import Path

p = Path(__file__).resolve().parents[1] / "src/main/resources/static/js/ui-only.js"
text = p.read_text(encoding="utf-8")

old = """    function buildOverlay() {
        overlay = document.createElement('motion');
        overlay.className = 'ncc-guide-overlay active';
        overlay.innerHTML =
            '<div class="ncc-guide-backdrop"></div>' +
            '<motion class="ncc-guide-spotlight"></motion>' +
            '<motion class="ncc-guide-panel">' +
            '  <h3></h3><p></p>' +
            '  <div class="ncc-guide-actions">' +
            '    <span class="ncc-guide-step-dots"></span>' +
            '    <button type="button" class="btn btn-sm btn-outline-light" data-guide-skip>Bỏ qua</button>' +
            '    <button type="button" class="btn btn-sm btn-danger" data-guide-next>Tiếp theo</button>' +
            '  </motion>' +
            '</motion>';
        overlay.innerHTML = overlay.innerHTML.replace(/<motion/g, '<div').replace(/<\\/motion>/g, '</motion>');
        overlay.innerHTML = overlay.innerHTML.replace(/<\\/motion>/g, '</motion>');
        document.body.appendChild(overlay);

        spotlight = overlay.querySelector('.ncc-guide-spotlight');
        panel = overlay.querySelector('.ncc-guide-panel');"""

old = old.replace("motion", "div").replace("</motion>", "</motion>")
# fix botched replace
old = """    function buildOverlay() {
        overlay = document.createElement('motion');
        overlay.className = 'ncc-guide-overlay active';
        overlay.innerHTML =
            '<div class="ncc-guide-backdrop"></div>' +
            '<div class="ncc-guide-spotlight"></motion>' +
            '<div class="ncc-guide-panel">' +
            '  <h3></h3><p></p>' +
            '  <div class="ncc-guide-actions">' +
            '    <span class="ncc-guide-step-dots"></span>' +
            '    <button type="button" class="btn btn-sm btn-outline-light" data-guide-skip>Bỏ qua</button>' +
            '    <button type="button" class="btn btn-sm btn-danger" data-guide-next>Tiếp theo</button>' +
            '  </div>' +
            '</motion>';
        overlay.innerHTML = overlay.innerHTML.replace(/<motion/g, '<div').replace(/<\\/motion>/g, '</motion>');
        overlay.innerHTML = overlay.innerHTML.replace(/<\\/motion>/g, '</motion>');
        document.body.appendChild(overlay);

        spotlight = overlay.querySelector('.ncc-guide-spotlight');
        panel = overlay.querySelector('.ncc-guide-panel');"""

# read actual file content for old block
start = text.index("    function buildOverlay()")
end = text.index("        overlay.querySelector('[data-guide-skip]')")
old = text[start:end]

new = """    function buildOverlay() {
        overlay = document.createElement('motion');
        overlay.className = 'ncc-guide-overlay active';

        const backdrop = document.createElement('motion');
        backdrop.className = 'ncc-guide-backdrop';
        overlay.appendChild(backdrop);

        spotlight = document.createElement('motion');
        spotlight.className = 'ncc-guide-spotlight';
        overlay.appendChild(spotlight);

        panel = document.createElement('motion');
        panel.className = 'ncc-guide-panel';
        const h3 = document.createElement('h3');
        const pEl = document.createElement('p');
        const actions = document.createElement('motion');
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
"""
new = new.replace("createElement('motion')", "createElement('div')")

text = text[:start] + new + text[end:]
# fix any remaining motion in file for createElement
text = text.replace("createElement('motion')", "createElement('div')")
p.write_text(text, encoding="utf-8")
print("fixed ui-only.js")
