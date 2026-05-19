from pathlib import Path

DIV = "motion"
DIV = "div"
o = "<" + DIV
c = "</" + DIV + ">"

snippet = f"""    {o} id="heroCarouselTmdb" class="carousel slide carousel-fade hero-carousel" data-bs-ride="carousel"
         th:if="${{heroSlides != null and !#lists.isEmpty(heroSlides)}}">
        {o} class="carousel-inner">
            {o} th:each="slide, stat : ${{heroSlides}}" class="carousel-item"
                 th:classappend="${{stat.first}} ? ' active' : ''">
                <img th:src="${{slide.backdropUrl != null ? slide.backdropUrl : slide.posterUrl}}"
                     class="d-block w-100 hero-banner-img" th:alt="${{slide.title}}"/>
                {o} class="carousel-caption hero-caption text-start">
                    <h2 th:text="${{slide.title}}">Phim</h2>
                    <p th:if="${{slide.overview != null}}" th:text="${{#strings.abbreviate(slide.overview, 160)}}"></p>
                    <a th:if="${{slide.movieId != null}}"
                       th:href="@{{/customer/movies/{{id}}(id=${{slide.movieId}})}}"
                       class="btn btn-danger btn-sm" th:text="#{{home.hero.book}}">Dat ve</a>
                    <a th:unless="${{slide.movieId != null}}"
                       th:href="@{{/customer/catalog}}"
                       class="btn btn-outline-light btn-sm" th:text="#{{home.hero.explore}}">Kham pha</a>
                {c}
            {c}
        {c}
        <button class="carousel-control-prev" type="button" data-bs-target="#heroCarouselTmdb" data-bs-slide="prev">
            <span class="carousel-control-prev-icon" aria-hidden="true"></span>
        </button>
        <button class="carousel-control-next" type="button" data-bs-target="#heroCarouselTmdb" data-bs-slide="next">
            <span class="carousel-control-next-icon" aria-hidden="true"></span>
        </button>
        {o} class="carousel-indicators">
            <button th:each="slide, stat : ${{heroSlides}}" type="button"
                    data-bs-target="#heroCarouselTmdb"
                    th:attr="data-bs-slide-to=${{stat.index}}"
                    th:classappend="${{stat.first}} ? ' active' : ''"></button>
        {c}
    {c}

"""

home = Path(__file__).resolve().parents[1] / "src/main/resources/templates/customer/home.html"
text = home.read_text(encoding="utf-8")
needle = '    <div id="carouselExampleFade" class="carousel slide carousel-fade" data-bs-ride="carousel">'
replacement = (
    f'    {o} id="carouselExampleFade" class="carousel slide carousel-fade" data-bs-ride="carousel" '
    f'th:unless="${{heroSlides != null and !#lists.isEmpty(heroSlides)}}">'
)
if "heroCarouselTmdb" not in text:
    text = text.replace(needle, snippet + replacement, 1)
    home.write_text(text, encoding="utf-8")
    print("patched")
else:
    print("already patched")
