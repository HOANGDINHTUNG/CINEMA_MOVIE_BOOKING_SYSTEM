from pathlib import Path

p = Path(__file__).resolve().parents[1] / "src/main/resources/templates/customer/home.html"
text = p.read_text(encoding="utf-8")
start = text.index('    <motion id="carouselExampleFade"'.replace("motion", "div"))
end = text.index('    <nav class="quick-nav"')

tag_o = "<" + "div"
tag_c = "</" + "motion>".replace("motion", "motion>")
tag_c = "</" + "motion>"
tag_c = "</div>"

new_carousel = f'''    {tag_o} id="carouselExampleFade" class="carousel slide carousel-fade hero-carousel" data-bs-ride="carousel"
         th:if="${{heroSlides != null and !#lists.isEmpty(heroSlides)}}">
        {tag_o} class="carousel-inner">
            {tag_o} th:each="slide, stat : ${{heroSlides}}" class="carousel-item"
                 th:classappend="${{stat.first}} ? ' active' : ''">
                <img th:src="${{slide.backdropUrl != null ? slide.backdropUrl : slide.posterUrl}}"
                     class="d-block w-100 hero-banner-img" th:alt="${{slide.title}}"/>
                {tag_o} class="carousel-caption hero-caption text-start">
                    <h2 th:text="${{slide.title}}">Phim</h2>
                    <p th:if="${{slide.overview != null}}" th:text="${{#strings.abbreviate(slide.overview, 160)}}"></p>
                    <a th:if="${{slide.movieId != null}}"
                       th:href="@{{/customer/movies/{{id}}(id=${{slide.movieId}})}}"
                       class="btn btn-danger btn-sm" th:text="#{{home.hero.book}}">Dat ve</a>
                    <a th:unless="${{slide.movieId != null}}"
                       th:href="@{{/customer/catalog}}"
                       class="btn btn-outline-light btn-sm" th:text="#{{home.hero.explore}}">Kham pha</a>
                {tag_c}
            {tag_c}
        {tag_c}
        <button class="carousel-control-prev" type="button" data-bs-target="#carouselExampleFade" data-bs-slide="prev">
            <span class="carousel-control-prev-icon" aria-hidden="true"></span>
            <span class="visually-hidden">Previous</span>
        </button>
        <button class="carousel-control-next" type="button" data-bs-target="#carouselExampleFade" data-bs-slide="next">
            <span class="carousel-control-next-icon" aria-hidden="true"></span>
            <span class="visually-hidden">Next</span>
        </button>
        {tag_o} class="carousel-indicators">
            <button th:each="slide, stat : ${{heroSlides}}" type="button"
                    data-bs-target="#carouselExampleFade"
                    th:attr="data-bs-slide-to=${{stat.index}}"
                    th:classappend="${{stat.first}} ? ' active' : ''"
                    th:aria-label="'Slide ' + ${{stat.index + 1}}"></button>
        {tag_c}
    {tag_c}
    {tag_o} id="carouselExampleFade" class="carousel slide carousel-fade" data-bs-ride="carousel"
         th:unless="${{heroSlides != null and !#lists.isEmpty(heroSlides)}}">
        {tag_o} class="carousel-inner">
            {tag_o} class="carousel-item active">
                <img th:src="@{{/assets/img/poster.jpg}}" class="d-block w-100 hero-banner-img" alt="banner"/>
            {tag_c}
            {tag_o} class="carousel-item">
                <img th:src="@{{/assets/img/events/Chuong-trinh-phim-he.webp}}" class="d-block w-100 hero-banner-img" alt=""/>
            {tag_c}
            {tag_o} class="carousel-item">
                <img th:src="@{{/assets/img/events/Lat-mat-banner.webp}}" class="d-block w-100 hero-banner-img" alt=""/>
            {tag_c}
        {tag_c}
        <button class="carousel-control-prev" type="button" data-bs-target="#carouselExampleFade" data-bs-slide="prev">
            <span class="carousel-control-prev-icon" aria-hidden="true"></span>
        </button>
        <button class="carousel-control-next" type="button" data-bs-target="#carouselExampleFade" data-bs-slide="next">
            <span class="carousel-control-next-icon" aria-hidden="true"></span>
        </button>
    {tag_c}

'''

# Fix thymeleaf - I doubled braces wrong. Rewrite file content manually as string without f-string mess
new_carousel = open(Path(__file__).parent / "home_carousel_snippet.html", encoding="utf-8").read() if (Path(__file__).parent / "home_carousel_snippet.html").exists() else ""

p.write_text(text[:start] + new_carousel + text[end:], encoding="utf-8")
print("done", len(new_carousel))
