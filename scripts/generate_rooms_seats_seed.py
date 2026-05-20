#!/usr/bin/env python3
"""Generate INSERT IGNORE for rooms + seats (diverse layouts)."""

from pathlib import Path

# (room_id, name, rows, seats_per_row, vip_rows_from_end) — vip_rows = last N rows are VIP
ROOMS = [
    (7, "Phòng 7 (Standard Plus)", 10, 14, 2),
    (8, "Phòng 8 (Premium 2K)", 8, 12, 2),
    (9, "Phòng 9 (Mega Screen)", 12, 16, 3),
    (10, "Phòng 10 (Compact)", 6, 10, 1),
    (11, "Phòng 11 (IMAX Laser)", 14, 18, 3),
    (12, "Phòng 12 (4DX Motion)", 9, 11, 2),
    (13, "Phòng 13 (Dolby Vision)", 7, 14, 2),
    (14, "Phòng 14 (Family)", 10, 10, 1),
    (15, "Phòng 15 (Couple Deluxe)", 5, 8, 5),
    (16, "Phòng 16 (Gold Class)", 11, 15, 4),
    (17, "Phòng 17 (Standard B)", 8, 8, 1),
    (18, "Phòng 18 (Ultra Wide)", 13, 12, 2),
    (19, "Phòng 19 (ScreenX)", 6, 14, 2),
    (20, "Phòng 20 (Classic Hall)", 10, 12, 2),
    (21, "Phòng 21 (Junior)", 5, 12, 0),
    (22, "Phòng 22 (Premiere)", 9, 16, 3),
    (23, "Phòng 23 (Luxe Recline)", 8, 10, 8),
    (24, "Phòng 24 (Open Air Sim)", 7, 11, 1),
    (25, "Phòng 25 (Festival)", 12, 14, 2),
    (26, "Phòng 26 (Indie)", 6, 9, 1),
    (27, "Phòng 27 (Blockbuster)", 11, 17, 3),
    (28, "Phòng 28 (Midnight)", 8, 13, 2),
    (29, "Phòng 29 (Kids Zone)", 7, 10, 0),
    (30, "Phòng 30 (Director's Cut)", 10, 15, 4),
    (31, "Phòng 31 (Starium)", 15, 20, 4),
    (32, "Phòng 32 (CineComfort)", 9, 13, 2),
    (33, "Phòng 33 (Mini VIP)", 4, 6, 4),
    (34, "Phòng 34 (Grand Platinum)", 12, 18, 5),
    (35, "Phòng 35 (Studio A)", 8, 11, 2),
    (36, "Phòng 36 (Studio B)", 8, 11, 2),
]

START_SEAT_ID = 93


def row_label(index: int) -> str:
    """0 -> A, 25 -> Z, 26 -> AA (if needed)."""
    n = index
    letters = ""
    while True:
        letters = chr(ord("A") + (n % 26)) + letters
        n = n // 26 - 1
        if n < 0:
            break
    return letters


def main() -> None:
    lines: list[str] = []
    lines.append("-- ========== ROOMS (mo rong 7-36) ==========")
    lines.append("-- Sinh boi scripts/generate_rooms_seats_seed.py")

    seat_id = START_SEAT_ID
    seat_lines: list[str] = []

    for room_id, name, rows, per_row, vip_from_end in ROOMS:
        total = rows * per_row
        lines.append(
            f"INSERT IGNORE INTO rooms (room_id, room_name, total_seats) "
            f"VALUES ({room_id},'{name}',{total});"
        )
        for r in range(rows):
            row_name = row_label(r)
            vip = vip_from_end > 0 and r >= rows - vip_from_end
            seat_type = "VIP" if vip else "STANDARD"
            for num in range(1, per_row + 1):
                seat_lines.append(
                    f"INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) "
                    f"VALUES ({seat_id},{room_id},'{row_name}',{num},'{seat_type}');"
                )
                seat_id += 1

    lines.append(f"-- ({len(ROOMS)} phong moi)")
    lines.append("")
    lines.append("-- ========== SEATS (mo rong, seat_id tu 93) ==========")
    lines.extend(seat_lines)
    lines.append(f"-- ({seat_id - START_SEAT_ID} ghe moi, tong seat_id den {seat_id - 1})")

    root = Path(__file__).resolve().parents[1]
    db_dir = root / "src" / "main" / "resources" / "db"
    fragment = "\n".join(lines) + "\n"
    frag_path = db_dir / "seed-rooms-extended.sql"
    frag_path.write_text(fragment, encoding="utf-8")

    seed_path = db_dir / "seed.sql"
    seed = seed_path.read_text(encoding="utf-8")
    start_marker = "-- ========== ROOMS (mo rong 7-36) =========="
    end_marker = "-- ========== COMBOS =========="
    if start_marker in seed and end_marker in seed:
        before = seed.split(start_marker)[0]
        after = seed.split(end_marker, 1)[1]
        seed = before.rstrip() + "\n\n" + fragment + end_marker + after
        seed_path.write_text(seed, encoding="utf-8")
        print(f"Merged into {seed_path}")
    else:
        marker = "-- (92 rows)\n-- ========== COMBOS =========="
        if marker in seed:
            seed = seed.replace(
                marker,
                "-- (92 ghe phong 1-6)\n" + fragment + end_marker,
                1,
            )
            seed_path.write_text(seed, encoding="utf-8")
            print(f"Merged into {seed_path} (first-time)")
        else:
            print("WARN: could not auto-merge; only wrote fragment")

    print(f"Wrote {frag_path}")
    print(f"Rooms: {len(ROOMS)}, Seats: {seat_id - START_SEAT_ID}")


if __name__ == "__main__":
    main()
