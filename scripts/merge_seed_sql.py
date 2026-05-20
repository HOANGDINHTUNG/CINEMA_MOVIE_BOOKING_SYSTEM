#!/usr/bin/env python3
"""Merge generated seed fragments into db/seed.sql."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED = ROOT / "src/main/resources/db/seed.sql"
FRAGMENTS = [
    ROOT / "src/main/resources/db/seed-content-articles.sql",
    ROOT / "src/main/resources/db/seed-movies-from-json.sql",
    ROOT / "src/main/resources/db/seed-users-from-json.sql",
]
MARKER = "-- ========== SEED FROM database/*.json"


def main():
    base = SEED.read_text(encoding="utf-8")
    if MARKER in base:
        base = base.split(MARKER)[0].rstrip() + "\n"
    parts = [base.rstrip(), "", MARKER + " (auto-generated) =========="]
    for frag in FRAGMENTS:
        if frag.exists():
            parts.append(frag.read_text(encoding="utf-8").strip())
    SEED.write_text("\n".join(parts) + "\n", encoding="utf-8")
    print(f"Updated {SEED}")


if __name__ == "__main__":
    main()
