from pathlib import Path

p = Path(__file__).resolve().parent.parent / "src/main/resources/templates/customer/home.html"
t = p.read_text(encoding="utf-8")
old = (
    '            <motion.div th:each="slide, stat : ${heroSlides}"\n'
    '                 th:class="\'carousel-item\' + (${stat.first} ? \' active\' : \'\')"\n'
    '                 th:replace="~{fragments/customer/hero-slide :: slide(${slide})}"></div>'
).replace("motion.", "")
new = (
    '            <div th:each="slide, stat : ${heroSlides}"\n'
    '                 th:class="\'carousel-item\' + (${stat.first} ? \' active\' : \'\')">\n'
    '                <div th:replace="~{fragments/customer/hero-slide :: slide(${slide})}"></div>\n'
    '            </div>'
).replace("motion.", "")
if old not in t:
    raise SystemExit("pattern not found")
p.write_text(t.replace(old, new), encoding="utf-8")
print("fixed")
