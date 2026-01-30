# Tests et benchmarks

Les scripts sont dans `tools/`.

## Lancer une suite locale

```bash
./tools/run_tests.sh 127.0.0.1 1337 MonEquipe
```

## Multi‑seeds (auto‑serveur)

```bash
AUTO_SERVER=1 ./tools/run_tests.sh 127.0.0.1 1337 MonEquipe tools/seeds.txt
```

Le script :

- compile l'IA,
- lance plusieurs presets (G1B0, G1B0P, G1AUTO),
- analyse les logs,
- produit un classement,
- calcule des moyennes normalisées par seed.

!!! warning
    Les logs et `tools/results.csv` sont écrasés à chaque run.

## Fichiers utiles

- `tools/run_tests.sh` : orchestration des presets.
- `tools/analyse_logs.py` : résumé tours/points/bonus.
- `tools/collect_results.py` : CSV par seed/preset.
- `tools/aggregate_results.py` : moyennes normalisées.
- `tools/seeds.txt` : liste des seeds.

## Interprétation des stats

- **tours** : nombre de tours joués.
- **points** : total des points.
- **bonus gain/use** : bonus ramassés / consommés.
- **bonus actions** : actions qui consomment un bonus.
- **streak sans points** : série max de tours sans gain.
- **va‑et‑vient** : retours sur la case d'avant‑avant.
- **positions uniques** : couverture du labyrinthe.

## Exemple de sortie (simplifiée)

```
=== Classement (tours asc, points desc) ===
testG1B0.txt: 83 tours, 620 pts
testG1AUTO.txt: 90 tours, 620 pts
...
=== Moyennes (tours asc sur seeds complets, ratio points desc) ===
G1B0: 104.23 tours, ratio points 1.000, wins 14, runs 30/30
G1B0P: 105.10 tours, ratio points 1.000, wins 10, runs 30/30
```

## Comprendre "wins"

- Un "win" = meilleur temps sur un seed **parmi les presets**.
- Un preset peut être légèrement plus lent en moyenne, mais gagner plus souvent.

## Conseils de benchmark

- Compare toujours sur les mêmes seeds.
- Regarde d'abord les points, puis les tours.
- Si tout le monde fait 100% des points, la vitesse est le seul critère.
- Si un preset ne termine pas, c'est un signal de bug.
