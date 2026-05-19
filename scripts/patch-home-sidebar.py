from pathlib import Path

TAG = "div"

p = Path(__file__).resolve().parents[1] / "src/main/resources/templates/customer/home.html"
text = p.read_text(encoding="utf-8")
start = text.index('<aside class="sidebar">')
end = text.index("</aside>", start) + len("</aside>")

o, c = f"<{TAG}", f"</{TAG}>"
new_block = f"""            <aside class="sidebar">
                {o} class="promotion">
                    {o} class="top-bar bar">
                        <h1>Khuyến mãi</h1>
                        <a th:href="@{{/customer/promotions}}" class="btn btn-sm btn-outline-light">Xem tất cả</a>
                    {c}
                    <a th:href="@{{/customer/promotions}}" class="card-container">
                        {o} class="box">
                            <img th:src="@{{/assets/img/promotions/promo1.png}}" alt=""/>
                            {o} class="description"><p>Ưu đãi đặc biệt hàng tuần</p>{c}
                        {c}
                    </a>
                {c}
                {o} class="event">
                    {o} class="top-bar">
                        <h1>Sự kiện &amp; tin tức</h1>
                        <a th:href="@{{/customer/news}}" class="btn btn-sm btn-outline-light">Xem tất cả</a>
                    {c}
                    <a th:href="@{{/customer/news}}" class="card-container">
                        {o} class="box">
                            <img th:src="@{{/assets/img/events/event3.jpg}}" alt=""/>
                            {o} class="description"><p>Chương trình phim hè</p>{c}
                        {c}
                    </a>
                {c}
            </aside>"""

p.write_text(text[:start] + new_block + text[end:], encoding="utf-8")
print("patched home sidebar")
