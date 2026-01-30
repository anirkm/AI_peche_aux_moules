# Logs et debug

## Logs client (superAI)

Active un log :

```bash
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe \
  log logFichier=IA/superAI/logs/test.txt
```

Contenu typique :

- Tour
- Position / arrivée
- Action envoyée
- Points gagnés
- Bonus gagnés/consommés
- Top 3 actions

Extrait court :

```
Tour 12
Position: (4,9)
Action: Bs-S
Arrivée: (4,11)
Gain points: 40
Bonus saut +/-: +0 / -1
...
```

## Logs serveur

Le serveur crée des fichiers `logServeurLaby-YYYYMMDD-HHMMSS` dans `Serveur/`.
Ils contiennent :

- les connexions,
- les messages reçus,
- les erreurs (mouvement invalide, format cassé),
- le résumé final (points par joueur).

## Lire un log vite

1. Cherche `Action:` pour voir ce que l'IA envoie.
2. Regarde `Gain points:` pour la progression.
3. Si tu vois beaucoup de `C`, l'IA passe souvent.
4. Si tu vois `Impossible de vous déplacer sur une case qui est un mur`, l'IA décode mal.

## Analyser automatiquement

```bash
python3 tools/analyse_logs.py IA/superAI/logs/test*.txt
```

Le script calcule tours, points, bonus, va‑et‑vient et positions uniques.

## Quand activer les logs

- Debug d'une régression.
- Comparaison de presets.
- Vérification d'un parsing.
