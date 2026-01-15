#!/usr/bin/env python3
import csv
import sys
from collections import defaultdict
from pathlib import Path


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: aggregate_results.py <results.csv>")
        return 1
    path = Path(sys.argv[1])
    if not path.exists():
        print("results file not found")
        return 1

    rows = []
    with path.open() as f:
        reader = csv.DictReader(f)
        for r in reader:
            rows.append(r)

    if not rows:
        print("no data")
        return 1

    # Normalise par seed (points max local = 1.0)
    by_seed = defaultdict(list)
    for r in rows:
        try:
            tours = int(r["tours"])
            points = int(r["points"])
        except (ValueError, KeyError):
            continue
        if tours <= 0:
            continue
        by_seed[r["seed"]].append((r["preset"], tours, points))

    agg = defaultdict(lambda: {
        "sum_tours_full": 0,
        "count_full": 0,
        "sum_ratio": 0.0,
        "count": 0,
        "wins": 0,
    })

    for seed, items in by_seed.items():
        max_points = max(p for _, _, p in items)
        best_tours = min(t for _, t, p in items if p == max_points)
        for preset, tours, points in items:
            ratio = points / max_points if max_points > 0 else 0.0
            agg[preset]["sum_ratio"] += ratio
            agg[preset]["count"] += 1
            if points == max_points:
                agg[preset]["sum_tours_full"] += tours
                agg[preset]["count_full"] += 1
                if tours == best_tours:
                    agg[preset]["wins"] += 1

    scored = []
    for preset, v in agg.items():
        avg_ratio = v["sum_ratio"] / v["count"] if v["count"] > 0 else 0.0
        if v["count_full"] > 0:
            avg_tours_full = v["sum_tours_full"] / v["count_full"]
        else:
            avg_tours_full = float("inf")
        scored.append((preset, avg_tours_full, avg_ratio, v["wins"], v["count"], v["count_full"]))

    scored.sort(key=lambda x: (x[1], -x[2]))

    print("=== Moyennes (tours asc sur seeds complets, ratio points desc) ===")
    for preset, avg_t, avg_ratio, wins, count, count_full in scored:
        tours_str = f"{avg_t:.2f}" if avg_t != float("inf") else "NA"
        print(f"{preset}: {tours_str} tours, ratio points {avg_ratio:.3f}, wins {wins}, "
              f"runs {count_full}/{count}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
