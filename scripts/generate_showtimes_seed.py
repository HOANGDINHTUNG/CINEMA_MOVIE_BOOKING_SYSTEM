#!/usr/bin/env python3
"""
Sinh / cập nhật showtimes + showtime_seats trong seed.sql.

Chế độ mặc định (--append-missing): chỉ thêm suất cho phim chưa có suất nào (giữ bookings).
Chế độ --regenerate-all: thay toàn bộ block SHOWTIMES (giữ phần từ BOOKINGS trở đi).

Chạy: python scripts/generate_showtimes_seed.py
      python scripts/generate_showtimes_seed.py --regenerate-all
"""
from __future__ import annotations

import argparse
import re
from datetime import datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED_SQL = ROOT / "src/main/resources/db/seed.sql"
OUT_FRAGMENT = ROOT / "src/main/resources/db/seed-showtimes-generated.sql"

SCHEDULE_DAYS = 10
CLEANING_BUFFER_MIN = 15
ROOM_IDS = list(range(1, 37))

SLOTS = [
    ("10:00:00", 75000),
    ("13:30:00", 85000),
    ("16:00:00", 90000),
    ("19:00:00", 105000),
    ("21:30:00", 115000),
]

SHOWTIMES_MARKERS = [
    "-- ========== SHOWTIMES (tu movies trong seed.sql) ==========",
    "-- ========== SHOWTIMES (tu movies",
    "-- ========== SHOWTIMES ==========",
]
BOOKINGS_MARKER = "-- ========== BOOKINGS =========="

MOVIE_RE = re.compile(
    r"INSERT IGNORE INTO movies \(tmdb_id, duration,[^)]+\) VALUES \((\d+), (\d+),",
    re.IGNORECASE,
)
SHOWTIME_TMDB_RE = re.compile(
    r"FROM movies m WHERE m\.tmdb_id = (\d+) LIMIT 1;",
    re.IGNORECASE,
)
SHOWTIME_ID_RE = re.compile(
    r"INSERT IGNORE INTO showtimes \(showtime_id, movie_id, room_id, start_time, end_time, base_price, status\)\s*\nSELECT (\d+),",
    re.IGNORECASE,
)


def parse_movies(seed_text: str) -> list[tuple[int, int]]:
    movies: list[tuple[int, int]] = []
    seen: set[int] = set()
    for m in MOVIE_RE.finditer(seed_text):
        tmdb_id, duration = int(m.group(1)), int(m.group(2))
        if tmdb_id in seen:
            continue
        seen.add(tmdb_id)
        movies.append((tmdb_id, max(duration, 1)))
    return movies


def parse_tmdb_with_showtimes(seed_text: str) -> set[int]:
    return {int(m.group(1)) for m in SHOWTIME_TMDB_RE.finditer(seed_text)}


def max_showtime_id(seed_text: str) -> int:
    ids = [int(m.group(1)) for m in SHOWTIME_ID_RE.finditer(seed_text)]
    return max(ids) if ids else 0


def overlaps(a_start: datetime, a_end: datetime, b_start: datetime, b_end: datetime) -> bool:
    return a_start < b_end and b_start < a_end


def fmt_dt(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S")


def try_place(
    room_schedules: dict[int, list[tuple[datetime, datetime]]],
    tmdb_id: int,
    duration: int,
    day: datetime,
    slot_index: int,
    time_str: str,
    price: float,
    room_id: int,
    showtime_id: int,
    earliest: datetime,
) -> dict | None:
    h, m, s = map(int, time_str.split(":"))
    start = day.replace(hour=h, minute=m, second=s)
    if start < earliest:
        return None
    end = start + timedelta(minutes=duration + CLEANING_BUFFER_MIN)
    for rs, re_ in room_schedules[room_id]:
        if overlaps(start, end, rs, re_):
            return None
    room_schedules[room_id].append((start, end))
    return {
        "showtime_id": showtime_id,
        "tmdb_id": tmdb_id,
        "room_id": room_id,
        "start": start,
        "end": end,
        "price": price,
    }


def ensure_one_per_movie(
    movies: list[tuple[int, int]],
    room_schedules: dict[int, list[tuple[datetime, datetime]]],
    showtimes: list[dict],
    showtime_id: int,
    base_day: datetime,
    earliest: datetime,
    only_tmdb: set[int] | None = None,
) -> int:
    """Giai đoạn 1: mỗi phim ít nhất một suất (nếu only_tmdb thì chỉ các phim đó)."""
    added = 0
    for tmdb_id, duration in movies:
        if only_tmdb is not None and tmdb_id not in only_tmdb:
            continue
        if only_tmdb is None:
            already = any(st["tmdb_id"] == tmdb_id for st in showtimes)
            if already:
                continue
        placed = False
        room_offset = abs(tmdb_id) % len(ROOM_IDS)
        for day_offset in range(SCHEDULE_DAYS):
            if placed:
                break
            day = base_day + timedelta(days=day_offset)
            for slot_index, (time_str, price) in enumerate(SLOTS):
                if placed:
                    break
                for room_try in range(len(ROOM_IDS)):
                    room_id = ROOM_IDS[(room_offset + slot_index + day_offset + room_try) % len(ROOM_IDS)]
                    st = try_place(
                        room_schedules,
                        tmdb_id,
                        duration,
                        day,
                        slot_index,
                        time_str,
                        price,
                        room_id,
                        showtime_id,
                        earliest,
                    )
                    if st:
                        showtimes.append(st)
                        showtime_id += 1
                        added += 1
                        placed = True
                        break
        if not placed:
            print(f"  WARN: khong dat duoc suat cho tmdb_id={tmdb_id}")
    return showtime_id


def generate_showtimes_all(movies: list[tuple[int, int]]) -> list[dict]:
    room_schedules: dict[int, list[tuple[datetime, datetime]]] = {r: [] for r in ROOM_IDS}
    showtimes: list[dict] = []
    showtime_id = 1
    base_day = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    earliest = datetime.now() + timedelta(hours=1)

    showtime_id = ensure_one_per_movie(
        movies, room_schedules, showtimes, showtime_id, base_day, earliest, only_tmdb=None
    )

    for tmdb_id, duration in movies:
        room_offset = abs(tmdb_id) % len(ROOM_IDS)
        for day_offset in range(SCHEDULE_DAYS):
            day = base_day + timedelta(days=day_offset)
            for slot_index, (time_str, price) in enumerate(SLOTS):
                placed = False
                for room_try in range(len(ROOM_IDS)):
                    room_id = ROOM_IDS[(room_offset + slot_index + day_offset + room_try) % len(ROOM_IDS)]
                    st = try_place(
                        room_schedules,
                        tmdb_id,
                        duration,
                        day,
                        slot_index,
                        time_str,
                        price,
                        room_id,
                        showtime_id,
                        earliest,
                    )
                    if st:
                        showtimes.append(st)
                        showtime_id += 1
                        placed = True
                        break
    return showtimes


def generate_missing_only(movies: list[tuple[int, int]], existing_tmdb: set[int]) -> list[dict]:
    missing = {tmdb_id for tmdb_id, _ in movies if tmdb_id not in existing_tmdb}
    if not missing:
        return []
    room_schedules: dict[int, list[tuple[datetime, datetime]]] = {r: [] for r in ROOM_IDS}
    showtimes: list[dict] = []
    base_day = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    earliest = datetime.now() + timedelta(hours=1)
    next_id = 1  # overwritten by caller via offset in build

    ensure_one_per_movie(
        movies,
        room_schedules,
        showtimes,
        1,
        base_day,
        earliest,
        only_tmdb=missing,
    )
    return showtimes


def build_sql(showtimes: list[dict], header_comment: str) -> str:
    lines = [
        header_comment,
        "-- Generated by scripts/generate_showtimes_seed.py",
        f"-- {len(showtimes)} suat",
        "",
    ]
    for st in showtimes:
        lines.append(
            "INSERT IGNORE INTO showtimes (showtime_id, movie_id, room_id, start_time, end_time, base_price, status)"
        )
        lines.append(
            f"SELECT {st['showtime_id']}, m.movie_id, {st['room_id']}, "
            f"'{fmt_dt(st['start'])}', '{fmt_dt(st['end'])}', {st['price']:.2f}, 'ACTIVE'"
        )
        lines.append(f"FROM movies m WHERE m.tmdb_id = {st['tmdb_id']} LIMIT 1;")
        lines.append("")

    if showtimes:
        lines.append("-- ========== SHOWTIME_SEATS (AVAILABLE — tu bang seats theo room) ==========")
        for st in showtimes:
            lines.append(
                "INSERT IGNORE INTO showtime_seats (showtime_id, seat_id, status, locked_by_user, locked_until)"
            )
            lines.append(
                f"SELECT {st['showtime_id']}, s.seat_id, 'AVAILABLE', NULL, NULL "
                f"FROM seats s WHERE s.room_id = {st['room_id']};"
            )
    return "\n".join(lines) + "\n"


def find_showtimes_start(text: str) -> int:
    for marker in SHOWTIMES_MARKERS:
        idx = text.find(marker)
        if idx >= 0:
            return idx
    m = list(MOVIE_RE.finditer(text))
    if not m:
        raise SystemExit("Khong tim thay movies trong seed.sql")
    cut_start = m[-1].end()
    while cut_start < len(text) and text[cut_start] in "\r\n":
        cut_start += 1
    return cut_start


def merge_replace_showtimes(seed_path: Path, fragment: str) -> None:
    text = seed_path.read_text(encoding="utf-8")
    cut_start = find_showtimes_start(text)
    bookings_idx = text.find(BOOKINGS_MARKER, cut_start)
    tail = text[bookings_idx:] if bookings_idx >= 0 else ""
    head = text[:cut_start].rstrip()
    seed_path.write_text(head + "\n\n" + fragment + ("\n" if tail else "") + tail, encoding="utf-8")


def append_before_bookings(seed_path: Path, fragment: str) -> None:
    text = seed_path.read_text(encoding="utf-8")
    bookings_idx = text.find(BOOKINGS_MARKER)
    if bookings_idx < 0:
        raise SystemExit("Khong tim thay marker BOOKINGS trong seed.sql")
    head = text[:bookings_idx].rstrip()
    tail = text[bookings_idx:]
    seed_path.write_text(head + "\n\n" + fragment + "\n" + tail, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--regenerate-all",
        action="store_true",
        help="Thay toan bo block SHOWTIMES (giu BOOKINGS)",
    )
    args = parser.parse_args()

    seed_text = SEED_SQL.read_text(encoding="utf-8")
    movies = parse_movies(seed_text)
    if not movies:
        raise SystemExit("Khong parse duoc movies tu seed.sql")

    if args.regenerate_all:
        showtimes = generate_showtimes_all(movies)
        for i, st in enumerate(showtimes, start=1):
            st["showtime_id"] = i
        header = "-- ========== SHOWTIMES (tu movies trong seed.sql) =========="
        fragment = build_sql(showtimes, header)
        OUT_FRAGMENT.write_text(fragment, encoding="utf-8")
        merge_replace_showtimes(SEED_SQL, fragment)
        print(f"Regenerated {len(showtimes)} showtimes for {len(movies)} movies")
        print(f"Merged into {SEED_SQL} (BOOKINGS preserved)")
        return

    existing = parse_tmdb_with_showtimes(seed_text)
    missing_count = len(movies) - len(existing)
    if missing_count == 0:
        print("Tat ca phim da co it nhat 1 suat — khong can them.")
        return

    new_rows = generate_missing_only(movies, existing)
    if not new_rows:
        print("Khong tao duoc suat moi.")
        return

    next_id = max_showtime_id(seed_text) + 1
    for st in new_rows:
        st["showtime_id"] = next_id
        next_id += 1

    header = "-- ========== SHOWTIMES (bo sung phim chua co suat) =========="
    fragment = build_sql(new_rows, header)
    OUT_FRAGMENT.write_text(fragment, encoding="utf-8")
    append_before_bookings(SEED_SQL, fragment)

    print(f"Movies: {len(movies)}, da co suat: {len(existing)}, them moi: {len(new_rows)} suat")
    print(f"Wrote fragment: {OUT_FRAGMENT}")
    print(f"Appended into: {SEED_SQL}")


if __name__ == "__main__":
    main()
