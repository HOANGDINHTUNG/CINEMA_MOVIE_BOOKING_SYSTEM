#!/usr/bin/env python3
"""
Lay 100 TMDB id now_playing -> admin «Dang doi lich chieu» (khong suat).
Lay them id trending (khong trung) -> demo co lich mau (tuy chon).

Sinh lai DemoTmdbCatalog.java va cap nhat application.properties.

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

TARGET_WAITING = 100
TARGET_SCHEDULED = 25
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


def collect_now_playing_only(api_key: str) -> list[int]:
    """Chi lay /movie/now_playing — khong trending/upcoming."""
    return collect_ids(
        api_key,
        "/movie/now_playing",
        {},
        TARGET_WAITING,
        region=REGION,
        max_pages=15,
    )


def collect_scheduled_demo(api_key: str, exclude: set[int]) -> list[int]:
    """Trending tuan — khong trung pool waiting."""
    seen = set(exclude)
    ordered: list[int] = []
    for mid in collect_ids(api_key, "/trending/movie/week", {}, 80, max_pages=10):
        if mid not in seen:
            seen.add(mid)
            ordered.append(mid)
        if len(ordered) >= TARGET_SCHEDULED:
            break
    return ordered[:TARGET_SCHEDULED]


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


def write_java(waiting_ids: list[int], scheduled_ids: list[int]) -> None:
    overlap = set(waiting_ids) & set(scheduled_ids)
    if overlap:
        raise SystemExit(f"ID trung waiting/scheduled: {overlap}")
    body = f"""package com.re.cinemamoviebookingsystem.config;

import java.util.List;
import java.util.Set;

/**
 * Danh sách TMDB id cho demo seed — sinh bởi scripts/fetch_tmdb_demo_catalog.py.
 * Waiting = now_playing ({len(waiting_ids)}). Scheduled = trending riêng ({len(scheduled_ids)}).
 */
public final class DemoTmdbCatalog {{

    private DemoTmdbCatalog() {{
    }}

    private static final Set<Long> WAITING_ID_SET = Set.copyOf(WAITING_SCHEDULE_TMDB_IDS);

{format_java_list("WAITING_SCHEDULE_TMDB_IDS", "TMDB now_playing — đăng không suất → Đang đợi lịch chiếu.", waiting_ids)}

{format_java_list("DEMO_SCHEDULED_TMDB_IDS", "Id riêng — lịch mẫu (không trùng waiting).", scheduled_ids)}

    static {{
        for (Long id : DEMO_SCHEDULED_TMDB_IDS) {{
            if (WAITING_ID_SET.contains(id)) {{
                throw new IllegalStateException("DEMO_SCHEDULED trùng WAITING: " + id);
            }}
        }}
    }}

    /** @deprecated Dùng {{@link #WAITING_SCHEDULE_TMDB_IDS}}. */
    @Deprecated
    public static final List<Long> NOW_SHOWING_TMDB_IDS = WAITING_SCHEDULE_TMDB_IDS;

    /** @deprecated Không seed upcoming cho đang đợi. */
    @Deprecated
    public static final List<Long> COMING_SOON_TMDB_IDS = List.of();

    public static boolean isWaitingPoolTmdbId(long tmdbId) {{
        return WAITING_ID_SET.contains(tmdbId);
    }}

    public static Set<Long> waitingIdSet() {{
        return WAITING_ID_SET;
    }}
}}
"""
    JAVA_OUT.write_text(body, encoding="utf-8")
    print(f"Wrote {JAVA_OUT}")


def patch_application_properties() -> None:
    text = APP_PROPS.read_text(encoding="utf-8")
    if "cinema.demo-seed-waiting-target" not in text:
        text = text.replace(
            "cinema.demo-seed-on-startup=true\n",
            "cinema.demo-seed-on-startup=true\n"
            f"cinema.demo-seed-waiting-target={TARGET_WAITING}\n"
            f"cinema.demo-seed-scheduled-target=0\n",
        )
    text = re.sub(
        r"cinema\.demo-seed-waiting-target=\d+",
        f"cinema.demo-seed-waiting-target={TARGET_WAITING}",
        text,
    )
    text = re.sub(
        r"cinema\.demo-seed-scheduled-target=\d+",
        "cinema.demo-seed-scheduled-target=0",
        text,
    )
    text = re.sub(
        r"cinema\.demo-seed-coming-soon-target=\d+",
        "cinema.demo-seed-coming-soon-target=0",
        text,
    )
    APP_PROPS.write_text(text, encoding="utf-8")
    print(f"Updated {APP_PROPS}")


def main() -> None:
    api_key = load_api_key()
    print("Fetching now_playing only (waiting pool)...")
    waiting_ids = collect_now_playing_only(api_key)
    print(f"  got {len(waiting_ids)} ids")
    print("Fetching scheduled demo (trending, exclude waiting)...")
    scheduled_ids = collect_scheduled_demo(api_key, set(waiting_ids))
    print(f"  got {len(scheduled_ids)} ids")
    if len(waiting_ids) < TARGET_WAITING:
        print(
            f"WARN: chi {len(waiting_ids)}/{TARGET_WAITING} now_playing",
            file=sys.stderr,
        )
    write_java(waiting_ids, scheduled_ids)
    patch_application_properties()


if __name__ == "__main__":
    main()
