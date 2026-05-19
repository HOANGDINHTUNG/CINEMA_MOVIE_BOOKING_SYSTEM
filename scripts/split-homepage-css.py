from pathlib import Path

root = Path(__file__).resolve().parent.parent / "src/main/resources/static/css/customer"
text = (root / "homepage.css").read_text(encoding="utf-8")
lines = text.splitlines(keepends=True)

layout = "".join(lines[:196])
hero = "".join(lines[196:402])

resp_lines = []
for i, line in enumerate(lines):
    n = i + 1
    if n < 461 or n > 896:
        continue
    low = line.lower()
    if "swiper" in low or "carouselexamplefade" in low:
        continue
    resp_lines.append(line)

footer = "".join(lines[1048:1062])
status = """
.home-section-status {
    color: #9aa5b5;
    font-size: 0.9rem;
    margin: 0 0 12px;
}
"""

(root / "home-layout.css").write_text(layout + status + footer, encoding="utf-8")
(root / "hero-banner.css").write_text("/* Hero banner */\n" + hero, encoding="utf-8")
(root / "home-responsive.css").write_text(
    "/* Responsive: home & catalog */\n" + "".join(resp_lines), encoding="utf-8"
)

aggregator = (
    "/* Home + catalog UI modules */\n"
    "@import url(\"home-layout.css\");\n"
    "@import url(\"hero-banner.css\");\n"
    "@import url(\"home-responsive.css\");\n"
)
(root / "homepage.css").write_text(aggregator, encoding="utf-8")
print("OK", len(layout), len(hero), len(resp_lines))
