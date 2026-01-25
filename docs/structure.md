# Structure du projet

```
.
├─ IA/
│  └─ superAI/
│     ├─ ClientSuperAI.java
│     ├─ IA.java
│     ├─ IAActions.java
│     ├─ IACibles.java
│     ├─ IABeam.java
│     ├─ Distances.java
│     ├─ MoteurSimulation.java
│     ├─ Journal.java
│     ├─ ParametresIA.java
│     └─ ...
├─ Serveur/
│  ├─ PecheAuxMoulesBoucle.java
│  ├─ PecheAuxMoules.java
│  └─ MG2D/
├─ tools/
│  ├─ run_tests.sh
│  ├─ analyse_logs.py
│  ├─ collect_results.py
│  └─ aggregate_results.py
├─ docs/
│  └─ (documentation MkDocs)
└─ mkdocs.yml
```

## IA/superAI (client)

- **ClientSuperAI** : client TCP (lecture -> décision -> envoi).
- **IA** : orchestration générale de la décision.
- **IAActions** : génération d’actions + scoring + pénalités.
- **IACibles** : choix de cibles + adversaires + concurrence.
- **IABeam** : recherche par faisceau (optionnelle).
- **Distances** : BFS classique + BFS avec bonus (saut / 3 pas).
- **MoteurSimulation** : applique une action localement.
- **ParametresIA** : valeurs par défaut et parsing CLI.
- **Journal** : logs de debug.
- **Mémoire*** : suivi des positions et verrouillage de cible.

## Serveur

- **PecheAuxMoulesBoucle** : serveur de référence.
- **PecheAuxMoules** : version de base (selon les fichiers fournis).
- **MG2D** : lib graphique pour l'interface.

## tools

- **run_tests.sh** : lance une suite de tests sur plusieurs presets.
- **analyse_logs.py** : résumé tours/points/bonus.
- **collect_results.py** : CSV par seed/preset.
- **aggregate_results.py** : moyennes normalisées.
- **seeds.txt** : seeds fixes pour comparaisons.

## docs

Documentation utilisateur + technique publiée sur GitHub Pages.
