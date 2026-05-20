#!/usr/bin/env python3
import re
from collections import Counter
from datetime import datetime, timedelta
from pathlib import Path

SEED = Path(__file__).resolve().parents[1] / "src/main/resources/db/seed.sql"
t = SEED.read_text(encoding="utf-8")

movie_re = re.compile(
    r"INSERT IGNORE INTO movies \(tmdb_id, duration,[^)]+\) VALUES \((\d+), (\d+),",
    re.I,
)
show_re = re.compile(
    r"SELECT \d+, m\.movie_id, \d+, '([^']+)', '([^']+)', [^,]+, 'ACTIVE'\s+FROM movies m WHERE m\.tmdb_id = (\d+)",
    re.I,
)

movies = {}  # tmdb -> duration
for m in movie_re.finditer(t):
    movies[m.group(1)] = int(m.group(2))

show_by_tmdb = Counter()
all_starts = []
for m in show_re.finditer(t):
    start_s, _end_s, tmdb = m.group(1), m.group(2), m.group(3)
    show_by_tmdb[tmdb] += 1
    all_starts.append(datetime.strptime(start_s, "%Y-%m-%d %H:%M:%S"))

with_show = set(show_by_tmdb.keys())
no_show = sorted(set(movies.keys()) - with_show, key=int)

now = datetime.now()
earliest_gen = now.replace(hour=0, minute=0, second=0, microsecond=0) + timedelta(hours=1)
future_starts = [s for s in all_starts if s > now]
past_starts = [s for s in all_starts if s <= now]

# Per movie: any future showtime?
future_by_tmdb = set()
for m in show_re.finditer(t):
    start = datetime.strptime(m.group(1), "%Y-%m-%d %H:%M:%S")
    if start > now:
        future_by_tmdb.add(m.group(3))

print("=== SEED.SQL ANALYSIS (now showing logic) ===")
print(f"Movies INSERT rows: {len(movies)}")
print(f"Showtime INSERT rows: {sum(show_by_tmdb.values())}")
print(f"Distinct tmdb_id WITH showtime in seed: {len(with_show)}")
print(f"Distinct tmdb_id WITHOUT any showtime: {len(no_show)}")
print()
print("App rule: ACTIVE movie + EXISTS showtime start_time > NOW + status ACTIVE/SOLD_OUT")
print(f"Movies with >=1 FUTURE showtime (as of {now.strftime('%Y-%m-%d %H:%M')}): {len(future_by_tmdb)}")
print()
if no_show:
    print("TMDB ids in movies but ZERO showtimes in seed:")
    print(", ".join(no_show))
    print()
print("Showtimes per movie: min=%s max=%s avg=%.1f" % (
    min(show_by_tmdb.values()), max(show_by_tmdb.values()),
    sum(show_by_tmdb.values()) / len(show_by_tmdb)))
print(f"All showtime starts: first={min(all_starts).date()} last={max(all_starts).date()}")
print(f"Future vs past slots: future={len(future_starts)} past={len(past_starts)} (total {len(all_starts)})")
