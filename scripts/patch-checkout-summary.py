from pathlib import Path

TAG = "motion"
p = Path(__file__).resolve().parents[1] / "src/main/resources/templates/customer/checkout.html"
text = p.read_text(encoding="utf-8")

needle = f"""                </{TAG}>
            </{TAG}>

            <{TAG} class="ticket-details" th:if="${{combos != null && !combos.isEmpty()}}">"""
needle = needle.replace("motion", "div")

insert = f"""                </div>
                <div class="checkout-summary mt-3" th:if="${{estimatedTotal != null}}">
                    <div class="info-label">Tạm tính (vé)</div>
                    <motion class="info-value bold highlight"
                         th:text="${{#numbers.formatDecimal(estimatedTotal, 0, 'COMMA', 0, 'POINT')}} + ' đ'"></div>
                    <p class="checkout-hint">Combo (nếu có) sẽ cộng thêm khi xác nhận.</p>
                </div>
            </div>

            <div class="ticket-details" th:if="${{combos != null && !combos.isEmpty()}}">"""
insert = insert.replace("<motion class=\"info-value", "<div class=\"info-value")

if needle not in text:
    raise SystemExit("needle not found in checkout.html")

p.write_text(text.replace(needle, insert, 1), encoding="utf-8")
print("patched checkout")
