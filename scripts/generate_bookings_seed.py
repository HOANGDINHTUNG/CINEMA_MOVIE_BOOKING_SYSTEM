#!/usr/bin/env python3
"""
Sinh bookings, tickets, booking_combos, payments + cap nhat showtime_seats (BOOKED)
tu showtimes/seats/combos/users trong seed.sql.

Chay: python scripts/generate_bookings_seed.py
"""
from __future__ import annotations

import random
import re
from collections import defaultdict
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED_SQL = ROOT / "src/main/resources/db/seed.sql"
OUT_FRAGMENT = ROOT / "src/main/resources/db/seed-bookings-generated.sql"

VIP_MULTIPLIER = Decimal("1.5")
TARGET_BOOKINGS = 320
CUSTOMER_USER_IDS = list(range(3, 13))
COMBO_OPTIONS = [
    (1, Decimal("75000")),
    (2, Decimal("115000")),
    (3, Decimal("220000")),
    (4, Decimal("55000")),
    (5, Decimal("165000")),
    (6, Decimal("89000")),
    (7, Decimal("135000")),
    (8, Decimal("200000")),
]
PAYMENT_METHODS = ("VNPAY", "MOMO", "CASH", "CARD")

SHOWTIME_RE = re.compile(
    r"SELECT (\d+), m\.movie_id, (\d+), '([^']+)', '([^']+)', ([\d.]+), 'ACTIVE'",
    re.IGNORECASE,
)
SEAT_RE = re.compile(
    r"INSERT IGNORE INTO seats \(seat_id, room_id, row_name, seat_number, seat_type\) "
    r"VALUES \((\d+),(\d+),'[^']*',\d+,'(STANDARD|VIP|DISABLED)'\);",
    re.IGNORECASE,
)

BOOKING_SECTION_MARKERS = (
    "\n-- 11) bookings",
    "\n-- Mau booking",
    "\nINSERT IGNORE INTO bookings (booking_id, user_id, showtime_id",
)


def parse_showtimes(text: str) -> list[dict]:
    out: list[dict] = []
    for m in SHOWTIME_RE.finditer(text):
        out.append(
            {
                "showtime_id": int(m.group(1)),
                "room_id": int(m.group(2)),
                "start_time": m.group(3),
                "end_time": m.group(4),
                "base_price": Decimal(m.group(5)),
            }
        )
    out.sort(key=lambda x: x["showtime_id"])
    return out


def parse_seats_by_room(text: str) -> dict[int, list[dict]]:
    rooms: dict[int, list[dict]] = defaultdict(list)
    for m in SEAT_RE.finditer(text):
        seat_id, room_id, seat_type = int(m.group(1)), int(m.group(2)), m.group(3).upper()
        if seat_type == "DISABLED":
            continue
        rooms[room_id].append({"seat_id": seat_id, "seat_type": seat_type})
    for room_id in rooms:
        rooms[room_id].sort(key=lambda s: s["seat_id"])
    return rooms


def seat_ticket_price(base: Decimal, seat_type: str) -> Decimal:
    if seat_type == "VIP":
        return (base * VIP_MULTIPLIER).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    return base.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def fmt_dt(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S")


def pick_status(rng: random.Random) -> str:
    roll = rng.random()
    if roll < 0.78:
        return "PAID"
    if roll < 0.90:
        return "PENDING"
    return "CANCELLED"


def generate_bookings(
    showtimes: list[dict], seats_by_room: dict[int, list[dict]], rng: random.Random
) -> tuple[list[dict], list[dict], list[dict], list[dict]]:
    if not showtimes:
        return [], [], [], []

    # Chi dat tren cac suat dau (de dashboard co du lieu, khong qua tai)
    pool = [st for st in showtimes if st["showtime_id"] <= min(600, showtimes[-1]["showtime_id"])]
    if not pool:
        pool = showtimes[:200]

    showtime_cursor = 0
    seat_offset: dict[int, int] = defaultdict(int)
    bookings: list[dict] = []
    tickets: list[dict] = []
    booking_combos: list[dict] = []
    payments: list[dict] = []

    booking_id = 1
    ticket_id = 1
    payment_id = 1
    base_booking_time = datetime.now().replace(microsecond=0) - timedelta(days=2)

    attempts = 0
    while len(bookings) < TARGET_BOOKINGS and attempts < TARGET_BOOKINGS * 8:
        attempts += 1
        st = pool[showtime_cursor % len(pool)]
        showtime_cursor += 1
        room_seats = seats_by_room.get(st["room_id"], [])
        if not room_seats:
            continue

        ticket_count = rng.choices([1, 2, 3, 4], weights=[45, 30, 18, 7], k=1)[0]
        off = seat_offset[st["showtime_id"]]
        if off + ticket_count > len(room_seats):
            continue
        # Toi da ~25% ghe/phong cho seed
        if off > max(8, int(len(room_seats) * 0.25)):
            continue

        chosen = room_seats[off : off + ticket_count]
        seat_offset[st["showtime_id"]] = off + ticket_count

        status = pick_status(rng)
        user_id = rng.choice(CUSTOMER_USER_IDS)
        booking_date = base_booking_time + timedelta(
            minutes=rng.randint(0, 60 * 48), seconds=rng.randint(0, 59)
        )

        ticket_total = Decimal("0")
        for seat in chosen:
            p = seat_ticket_price(st["base_price"], seat["seat_type"])
            ticket_total += p

        combo_row = None
        if status == "PAID" and rng.random() < 0.42:
            combo_id, combo_price = rng.choice(COMBO_OPTIONS)
            qty = 1 if rng.random() < 0.85 else 2
            combo_row = {
                "booking_id": booking_id,
                "combo_id": combo_id,
                "quantity": qty,
                "price": combo_price,
                "line_total": combo_price * qty,
            }
            booking_combos.append(combo_row)

        combo_total = combo_row["line_total"] if combo_row else Decimal("0")
        total_amount = (ticket_total + combo_total).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

        bookings.append(
            {
                "booking_id": booking_id,
                "user_id": user_id,
                "showtime_id": st["showtime_id"],
                "booking_date": booking_date,
                "total_amount": total_amount,
                "status": status,
            }
        )

        for idx, seat in enumerate(chosen, start=1):
            price = seat_ticket_price(st["base_price"], seat["seat_type"])
            tickets.append(
                {
                    "ticket_id": ticket_id,
                    "booking_id": booking_id,
                    "seat_id": seat["seat_id"],
                    "ticket_code": f"TKT-{booking_id:05d}-{idx:02d}",
                    "price": price,
                    "showtime_id": st["showtime_id"],
                    "booking_status": status,
                }
            )
            ticket_id += 1

        if status == "PAID":
            payments.append(
                {
                    "payment_id": payment_id,
                    "booking_id": booking_id,
                    "payment_method": rng.choice(PAYMENT_METHODS),
                    "transaction_id": f"TXN-SEED-{payment_id:06d}",
                    "amount": total_amount,
                    "payment_status": "SUCCESS",
                    "payment_date": booking_date + timedelta(minutes=rng.randint(1, 15)),
                }
            )
            payment_id += 1
        elif status == "PENDING" and rng.random() < 0.65:
            payments.append(
                {
                    "payment_id": payment_id,
                    "booking_id": booking_id,
                    "payment_method": rng.choice(("VNPAY", "MOMO")),
                    "transaction_id": f"TXN-PEND-{payment_id:06d}",
                    "amount": total_amount,
                    "payment_status": "PENDING",
                    "payment_date": None,
                }
            )
            payment_id += 1

        booking_id += 1

    return bookings, tickets, booking_combos, payments


def build_sql(
    bookings: list[dict],
    tickets: list[dict],
    booking_combos: list[dict],
    payments: list[dict],
) -> str:
    lines = [
        "-- ========== BOOKINGS / TICKETS (tu showtimes + seats) ==========",
        "-- Generated by scripts/generate_bookings_seed.py",
        f"-- {len(bookings)} don / {len(tickets)} ve / {len(booking_combos)} combo / {len(payments)} thanh toan",
        "",
        "-- 11) bookings — don dat ve (PENDING | PAID | CANCELLED)",
    ]
    for b in bookings:
        lines.append(
            "INSERT IGNORE INTO bookings (booking_id, user_id, showtime_id, booking_date, total_amount, status)"
        )
        lines.append(
            f"VALUES ({b['booking_id']}, {b['user_id']}, {b['showtime_id']}, "
            f"'{fmt_dt(b['booking_date'])}', {b['total_amount']}, '{b['status']}');"
        )

    lines.append("")
    lines.append("-- 12) tickets — ve theo ghe (ticket_code UNIQUE)")
    for t in tickets:
        lines.append(
            "INSERT IGNORE INTO tickets (ticket_id, booking_id, seat_id, ticket_code, price)"
        )
        lines.append(
            f"VALUES ({t['ticket_id']}, {t['booking_id']}, {t['seat_id']}, "
            f"'{t['ticket_code']}', {t['price']});"
        )

    if booking_combos:
        lines.append("")
        lines.append("-- 13) booking_combos — combo kem don")
        for bc in booking_combos:
            lines.append(
                "INSERT IGNORE INTO booking_combos (booking_id, combo_id, quantity, price)"
            )
            lines.append(
                f"VALUES ({bc['booking_id']}, {bc['combo_id']}, {bc['quantity']}, {bc['price']});"
            )

    if payments:
        lines.append("")
        lines.append("-- 14) payments — thanh toan")
        for p in payments:
            pay_date = "NULL" if p["payment_date"] is None else f"'{fmt_dt(p['payment_date'])}'"
            lines.append(
                "INSERT IGNORE INTO payments (payment_id, booking_id, payment_method, "
                "transaction_id, amount, payment_status, payment_date)"
            )
            lines.append(
                f"VALUES ({p['payment_id']}, {p['booking_id']}, '{p['payment_method']}', "
                f"'{p['transaction_id']}', {p['amount']}, '{p['payment_status']}', {pay_date});"
            )

    lines.extend(
        [
            "",
            "-- Cap nhat ghe da ban (chi don PAID)",
            "UPDATE showtime_seats ss",
            "INNER JOIN tickets t ON t.seat_id = ss.seat_id",
            "INNER JOIN bookings b ON b.booking_id = t.booking_id AND b.showtime_id = ss.showtime_id",
            "SET ss.status = 'BOOKED', ss.locked_by_user = NULL, ss.locked_until = NULL",
            "WHERE b.status = 'PAID';",
            "",
            "-- 15) audit_logs — mau (giu 1 dong tham chieu)",
            "INSERT IGNORE INTO audit_logs (id, user_id, username, action, entity_type, entity_id, detail, created_at)",
            "VALUES (90001, 1, 'admin_demo', 'SEED_BOOKINGS_GENERATED', 'BOOKING', '1', "
            "'Sinh hang loat bookings/tickets tu showtimes seed', NOW());",
            "",
        ]
    )
    return "\n".join(lines) + "\n"


def merge_into_seed(seed_path: Path, fragment: str) -> None:
    text = seed_path.read_text(encoding="utf-8")
    cut_start = -1
    for marker in BOOKING_SECTION_MARKERS:
        idx = text.find(marker)
        if idx >= 0:
            cut_start = idx
            break
    if cut_start < 0:
        # Sau showtime_seats cuoi
        marker = "FROM seats s WHERE s.room_id ="
        last = text.rfind(marker)
        if last < 0:
            raise SystemExit("Khong tim thay vi tri chen bookings trong seed.sql")
        line_end = text.find("\n", last)
        cut_start = line_end if line_end >= 0 else len(text)

    new_text = text[:cut_start].rstrip() + "\n\n" + fragment
    seed_path.write_text(new_text, encoding="utf-8")


def main() -> None:
    seed_text = SEED_SQL.read_text(encoding="utf-8")
    showtimes = parse_showtimes(seed_text)
    seats_by_room = parse_seats_by_room(seed_text)
    if not showtimes:
        raise SystemExit("Khong parse duoc showtimes tu seed.sql")
    if not seats_by_room:
        raise SystemExit("Khong parse duoc seats tu seed.sql")

    rng = random.Random(42)
    bookings, tickets, booking_combos, payments = generate_bookings(
        showtimes, seats_by_room, rng
    )
    fragment = build_sql(bookings, tickets, booking_combos, payments)
    OUT_FRAGMENT.write_text(fragment, encoding="utf-8")
    merge_into_seed(SEED_SQL, fragment)

    paid = sum(1 for b in bookings if b["status"] == "PAID")
    print(f"Showtimes parsed: {len(showtimes)}")
    print(f"Rooms with seats: {len(seats_by_room)}")
    print(f"Bookings: {len(bookings)} (PAID={paid})")
    print(f"Tickets: {len(tickets)}")
    print(f"Wrote fragment: {OUT_FRAGMENT}")
    print(f"Merged into: {SEED_SQL}")


if __name__ == "__main__":
    main()
