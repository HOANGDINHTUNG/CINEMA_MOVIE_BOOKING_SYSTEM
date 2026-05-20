#!/usr/bin/env python3
"""
Lay 100 TMDB id phim dang hot + 100 phim sap chieu tu TMDB API.
Sinh lai DemoTmdbCatalog.java va cap nhat application.properties targets.

Can: tmdb.api-key trong application-local.properties hoac bien TMDB_API_KEY.
"""
from __future__ import annotations

import gzip
import json
import os
import re
import sys
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCAL_PROPS = ROOT / "src/main/resources/application-local.properties"
JAVA_OUT = ROOT / "src/main/java/com/re/cinemamoviebookingsystem/config/DemoTmdbCatalog.java"
APP_PROPS = ROOT / "src/main/resources/application.properties"

TARGET_NOW = 100
TARGET_SOON = 100
LANG = "vi-VN"
REGION = "VN"


def load_api_key() -> str:
    key = os.environ.get("TMDB_API_KEY") or os.environ.get("VITE_TMDB_V3_KEY")
    if key:
        return key.strip()
    if LOCAL_PROPS.exists():
        text = LOCAL_PROPS.read_text(encoding="utf-8")
        m = re.search(r"^tmdb\.api-key\s*=\s*(.+)$", text, re.MULTILINE)
        if m and m.group(1).strip() and "YOUR_" not in m.group(1):
            return m.group(1).strip()
    raise SystemExit(
        "Chua co TMDB API key. Dat trong application-local.properties hoac TMDB_API_KEY."
    )


def tmdb_get(api_key: str, path: str, params: dict | None = None) -> dict:
    q = {"api_key": api_key, "language": LANG}
    if params:
        q.update(params)
    url = "https://api.themoviedb.org/3" + path + "?" + urllib.parse.urlencode(q)
    req = urllib.request.Request(
        url,
        headers={"Accept": "application/json", "Accept-Encoding": "identity"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        raw = resp.read()
        if raw[:2] == b"\x1f\x8b":
            raw = gzip.decompress(raw)
        return json.loads(raw.decode("utf-8"))


def collect_ids(
    api_key: str,
    path: str,
    extra_params: dict,
    limit: int,
    *,
    region: str | None = None,
    max_pages: int = 20,
) -> list[int]:
    seen: set[int] = set()
    ordered: list[int] = []
    page = 1
    while len(ordered) < limit and page <= max_pages:
        params = {"page": str(page), **extra_params}
        if region:
            params["region"] = region
        data = tmdb_get(api_key, path, params)
        results = data.get("results") or []
        if not results:
            break
        for item in results:
            mid = item.get("id")
            if mid is None or mid in seen:
                continue
            seen.add(mid)
            ordered.append(int(mid))
            if len(ordered) >= limit:
                break
        if page >= data.get("total_pages", page):
            break
        page += 1
    return ordered


def collect_now_showing(api_key: str) -> list[int]:
    seen: set[int] = set()
    ordered: list[int] = []

    def add_from(path: str, params: dict, max_each: int, *, region: str | None = REGION):
        nonlocal ordered
        for mid in collect_ids(api_key, path, params, max_each, region=region):
            if mid not in seen:
                seen.add(mid)
                ordered.append(mid)
            if len(ordered) >= TARGET_NOW:
                return

    add_from("/trending/movie/week", {}, 60)
    add_from("/movie/now_playing", {}, 60)
    add_from(
        "/discover/movie",
        {
            "sort_by": "popularity.desc",
            "vote_count.gte": "100",
            "vote_average.gte": "6.0",
            "include_adult": "false",
            "primary_release_date.lte": "2026-12-31",
        },
        60,
    )
    return ordered[:TARGET_NOW]


def collect_coming_soon(api_key: str, now_ids: set[int]) -> list[int]:
    from datetime import date, timedelta

    today = date.today().isoformat()
    in_12m = (date.today() + timedelta(days=365)).isoformat()
    seen: set[int] = set(now_ids)
    ordered: list[int] = []

    def add_from(
        path: str,
        params: dict,
        max_each: int,
        *,
        region: str | None = None,
    ):
        nonlocal ordered
        for mid in collect_ids(
            api_key, path, params, max_each, region=region, max_pages=25
        ):
            if mid not in seen:
                seen.add(mid)
                ordered.append(mid)
            if len(ordered) >= TARGET_SOON:
                return

    # Upcoming chinh thuc (VN + US de du phong)
    add_from("/movie/upcoming", {}, 120, region=REGION)
    add_from("/movie/upcoming", {}, 120, region="US")

    # Discover: phim chua chieu / sap chieu, xep theo do pho bien
    discover_future = {
        "sort_by": "popularity.desc",
        "include_adult": "false",
        "include_video": "false",
        "vote_count.gte": "15",
        "primary_release_date.gte": today,
        "primary_release_date.lte": in_12m,
    }
    add_from("/discover/movie", discover_future, 150)
    add_from(
        "/discover/movie",
        {
            **discover_future,
            "sort_by": "release_date.desc",
            "with_release_type": "2|3",
        },
        150,
    )
    add_from(
        "/discover/movie",
        {
            "sort_by": "popularity.desc",
            "vote_count.gte": "50",
            "primary_release_date.gte": today,
            "with_release_type": "3",
        },
        150,
        region="US",
    )
    return ordered[:TARGET_SOON]


def format_java_list(name: str, comment: str, ids: list[int]) -> str:
    lines = [f"    /** {comment} */", f"    public static final List<Long> {name} = List.of("]
    chunk = []
    for i, mid in enumerate(ids):
        chunk.append(f"{mid}L")
        if len(chunk) == 10 or i == len(ids) - 1:
            lines.append("            " + ", ".join(chunk) + ("," if i < len(ids) - 1 else ""))
            chunk = []
    lines.append("    );")
    return "\n".join(lines)


def write_java(now_ids: list[int], soon_ids: list[int]) -> None:
    body = f"""package com.re.cinemamoviebookingsystem.config;

import java.util.List;

/**
 * Danh sách TMDB id cho demo seed — sinh tự động bởi scripts/fetch_tmdb_demo_catalog.py.
 * Cập nhật: {len(now_ids)} đang chiếu (hot) + {len(soon_ids)} sắp chiếu.
 */
public final class DemoTmdbCatalog {{

    private DemoTmdbCatalog() {{
    }}

{format_java_list("NOW_SHOWING_TMDB_IDS", "Đăng kèm lịch chiếu → «Phim đang chiếu» (hot / trending / now playing).", now_ids)}

{format_java_list("COMING_SOON_TMDB_IDS", "Đăng không tạo suất → «Phim sắp chiếu» (upcoming hot).", soon_ids)}
}}
"""
    JAVA_OUT.write_text(body, encoding="utf-8")
    print(f"Wrote {JAVA_OUT} ({len(now_ids)} now, {len(soon_ids)} soon)")


def patch_application_properties() -> None:
    text = APP_PROPS.read_text(encoding="utf-8")
    text = re.sub(
        r"cinema\.demo-seed-now-showing-target=\d+",
        f"cinema.demo-seed-now-showing-target={TARGET_NOW}",
        text,
    )
    text = re.sub(
        r"cinema\.demo-seed-coming-soon-target=\d+",
        f"cinema.demo-seed-coming-soon-target={TARGET_SOON}",
        text,
    )
    APP_PROPS.write_text(text, encoding="utf-8")
    print(f"Updated {APP_PROPS} targets to {TARGET_NOW}/{TARGET_SOON}")


def main() -> None:
    api_key = load_api_key()
    print("Fetching now showing (hot)...")
    now_ids = collect_now_showing(api_key)
    print(f"  got {len(now_ids)} ids")
    print("Fetching coming soon (hot)...")
    soon_ids = collect_coming_soon(api_key, set(now_ids))
    print(f"  got {len(soon_ids)} ids")
    if len(now_ids) < TARGET_NOW:
        print(f"WARN: chi lay duoc {len(now_ids)}/{TARGET_NOW} phim dang chieu", file=sys.stderr)
    if len(soon_ids) < TARGET_SOON:
        print(f"WARN: chi lay duoc {len(soon_ids)}/{TARGET_SOON} phim sap chieu", file=sys.stderr)
    write_java(now_ids, soon_ids)
    patch_application_properties()


if __name__ == "__main__":
    main()
