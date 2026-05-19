from pathlib import Path

p = Path(__file__).resolve().parents[1] / "src/main/resources/templates/customer/seats.html"
text = p.read_text(encoding="utf-8")
old = """                <div class="footer1">
                    <motion class="auth-buttons1">
                        <a th:if="${seatMap.movieId != null}"
                           th:href="@{/customer/movies/{id}(id=${seatMap.movieId})}" class="btn-outlinee1">Quay lại</a>
                        <a th:if="${seatMap.movieId == null}" th:href="@{/customer/home}" class="btn-outlinee1">Quay lại</a>
                        <button type="submit" class="btn-primary1" th:disabled="${seatMap.soldOut}">Tiếp tục thanh toán</button>
                    </motion>
                </motion>
            </form>
        </motion>"""
old = old.replace("<motion", "<div").replace("</motion>", "</div>")

new = """            </form>
        </div>

        <div class="seat-checkout-bar" id="seatCheckoutBar" th:if="${!seatMap.soldOut}">
            <div class="seat-checkout-summary">
                <p class="seat-summary-title" id="seatSummaryTitle">Chưa chọn ghế</p>
                <p class="seat-summary-detail" id="seatSummaryDetail"></p>
                <p class="seat-summary-total" id="seatSummaryTotal"></p>
            </div>
            <div class="seat-checkout-actions">
                <a th:if="${seatMap.movieId != null}"
                   th:href="@{/customer/movies/{id}(id=${seatMap.movieId})}" class="btn-outlinee1">Quay lại</a>
                <a th:if="${seatMap.movieId == null}" th:href="@{/customer/home}" class="btn-outlinee1">Trang chủ</a>
                <button type="submit" form="seatForm" class="btn-primary1" id="seatSubmitBtn" disabled>Tiếp tục thanh toán</button>
            </div>
        </motion>"""
new = new.replace("</motion>", "</motion>").replace("<motion", "<div")
new = new.replace("</motion>", "</div>")

if old not in text:
    raise SystemExit("block not found")
p.write_text(text.replace(old, new, 1), encoding="utf-8")
print("ok")
