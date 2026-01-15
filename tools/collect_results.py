#!/usr/bin/env python3
import re
import sys
from pathlib import Path

RE_POINTS = re.compile(r"^Gain points: (\d+)")


def read_points(path: Path) -> int:
    total = 0
    for line in path.read_text().splitlines():
        m = RE_POINTS.match(line)
        if m:
            total += int(m.group(1))
    return total


def read_tours(path: Path) -> int:
    return sum(1 for line in path.read_text().splitlines() if line.startswith("Tour "))


def main() -> int:
    if len(sys.argv) < 3:
        print("usage: collect_results.py <seedId> <log1> [log2...]", file=sys.stderr)
        return 1
    seed_id = sys.argv[1]
    for p in sys.argv[2:]:
        path = Path(p)
        if not path.exists():
            continue
        name = path.stem.replace("test", "")
        tours = read_tours(path)
        points = read_points(path)
        if tours <= 0:
            # Log incomplet ou run rate.
            continue
        print(f"{seed_id},{name},{tours},{points}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
