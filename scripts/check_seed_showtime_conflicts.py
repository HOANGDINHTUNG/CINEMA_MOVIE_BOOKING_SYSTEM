#!/usr/bin/env python3
import re
from collections import defaultdict
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED = ROOT / "src/main/resources/db/seed.sql"
text = SEED.read_text(encoding="utf-8")
pat = re.compile(r"SELECT (\d+), m\.movie_id, (\d+), '([^']+)', '([^']+)'", re.I)
rows = pat.findall(text)

def parse(s):
    return datetime.strptime(s, "%Y-%m-%d %H:%M:%S")

items = [(int(sid), int(room), parse(st), parse(en)) for sid, room, st, en in rows]
by_room_start = defaultdict(list)
for it in items:
    by_room_start[(it[1], it[2])].append(it)

print("total", len(items))
print("exact duplicate room+start", sum(1 for v in by_room_start.values() if len(v) > 1))
for k, v in list((k, v) for k, v in by_room_start.items() if len(v) > 1)[:10]:
    print("DUP", k, v)

overlaps = []
by_room = defaultdict(list)
for it in items:
    by_room[it[1]].append(it)
for room, lst in by_room.items():
    lst.sort(key=lambda x: x[2])
    for i in range(len(lst)):
        for j in range(i + 1, len(lst)):
            a, b = lst[i], lst[j]
            if a[2] < b[3] and b[2] < a[3]:
                overlaps.append((room, a, b))
            if b[2] >= a[3]:
                break
print("interval overlaps", len(overlaps))
for o in overlaps[:25]:
    print(o)
