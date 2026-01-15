import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Petit parseur de logs, volontairement simple.
RE_TOUR = re.compile(r"^Tour \d+")
RE_POINTS = re.compile(r"^Gain points: (\d+)")
RE_ACTION = re.compile(r"^Action: (.+)")
RE_ARRIVEE = re.compile(r"^Arrivee: \((\d+),(\d+)\)")
RE_BONUS_SAUT = re.compile(r"^Bonus saut \+/-: \+(\d+) / -(\d+)")
RE_BONUS_TROIS = re.compile(r"^Bonus 3 pas \+/-: \+(\d+) / -(\d+)")


def analyser(fichier: Path) -> dict:
    lignes = fichier.read_text().splitlines()
    tours = 0
    points = []
    actions = []
    positions = []
    gain_saut = 0
    use_saut = 0
    gain_trois = 0
    use_trois = 0

    for ligne in lignes:
        if RE_TOUR.match(ligne):
            tours += 1
            continue

        m = RE_POINTS.match(ligne)
        if m:
            points.append(int(m.group(1)))
            continue

        m = RE_ACTION.match(ligne)
        if m:
            actions.append(m.group(1).strip())
            continue

        m = RE_ARRIVEE.match(ligne)
        if m:
            positions.append((int(m.group(1)), int(m.group(2))))
            continue

        m = RE_BONUS_SAUT.match(ligne)
        if m:
            gain_saut += int(m.group(1))
            use_saut += int(m.group(2))
            continue

        m = RE_BONUS_TROIS.match(ligne)
        if m:
            gain_trois += int(m.group(1))
            use_trois += int(m.group(2))
            continue

    total_points = sum(points)

    # Plus longue serie sans points
    best = 0
    streak = 0
    for p in points:
        if p == 0:
            streak += 1
        else:
            best = max(best, streak)
            streak = 0
    best = max(best, streak)

    # Va-et-vient (retour sur la case d'avant-avant)
    back = 0
    for i in range(2, len(positions)):
        if positions[i] == positions[i - 2]:
            back += 1

    c_actions = sum(1 for a in actions if a.startswith("C"))
    bonus_actions = sum(1 for a in actions if a.startswith("Bs") or a.startswith("Bp"))
    unique_positions = len(set(positions)) if positions else 0

    return {
        "tours": tours if tours > 0 else len(points),
        "points": total_points,
        "gain_saut": gain_saut,
        "use_saut": use_saut,
        "gain_trois": gain_trois,
        "use_trois": use_trois,
        "bonus_actions": bonus_actions,
        "c_actions": c_actions,
        "streak_zero": best,
        "backtracks": back,
        "unique_positions": unique_positions,
    }


def main() -> int:
    fichiers = [Path(p) for p in sys.argv[1:]]
    if not fichiers:
        fichiers = sorted((ROOT / "IA/superAI/logs").glob("test*.txt"))
    if not fichiers:
        print("Aucun fichier de log a analyser.")
        return 1

    resumes = []

    for f in fichiers:
        if not f.exists():
            print(f"Fichier introuvable: {f}")
            continue
        stats = analyser(f)
        resumes.append((f.name, stats))
        print(f"=== {f.name} ===")
        print(f"tours: {stats['tours']}")
        print(f"points: {stats['points']}")
        print(f"bonus saut gain/use: {stats['gain_saut']} / {stats['use_saut']}")
        print(f"bonus 3 pas gain/use: {stats['gain_trois']} / {stats['use_trois']}")
        print(f"bonus actions: {stats['bonus_actions']}")
        print(f"C actions: {stats['c_actions']}")
        print(f"streak sans points (max): {stats['streak_zero']}")
        print(f"va-et-vient: {stats['backtracks']}")
        print(f"positions uniques: {stats['unique_positions']}")
        print()

    if len(resumes) > 1:
        resumes.sort(key=lambda x: (x[1]["tours"], -x[1]["points"]))
        print("=== Classement (tours asc, points desc) ===")
        for name, stats in resumes:
            print(f"{name}: {stats['tours']} tours, {stats['points']} pts")
        print()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
