# Paramètres CLI

Tous les paramètres sont optionnels. Format : `cle=valeur`.

## Paramètres IA (défauts)

| Clé | Défaut | Description rapide |
| --- | --- | --- |
| `valeurSaut` | 25.0 | Valeur d'un bonus saut. |
| `valeurTroisPas` | 35.0 | Valeur d'un bonus 3 pas. |
| `penaliteUtilisationSaut` | 10.0 | Coût d'utilisation d'un saut. |
| `penaliteUtilisationTroisPas` | 14.0 | Coût d'utilisation d'un 3 pas. |
| `penaliteDistance` | 1.6 | Pénalise la distance aux cibles. |
| `coeffProfondeur` | 0.7 | Poids du futur proche. |
| `penaliteImmobile` | 6.0 | Pénalise les tours immobiles. |
| `penaliteRetour` | 3.5 | Pénalise les demi‑tours. |
| `penaliteBoucle` | 7.0 | Pénalise les cycles courts. |
| `verrouillageCible` | 4 | Nombre de tours avant relâche de la cible. |
| `seuilChangementCible` | 1.3 | Ratio pour forcer un changement de cible. |
| `modeHybride` | 1 | Active le mode hybride. |
| `modeCompromis` | 1 | Compromis points/distance. |
| `margeCompromis` | 0.15 | Tolère des cibles un peu moins rentables. |
| `margeCompromisFin` | 0.05 | Version plus stricte en fin de partie. |
| `modePlan` | 1 | Active la planification locale. |
| `nbCiblesPlan` | 8 | Taille du pool de cibles. |
| `profondeurPlan` | 4 | Longueur du plan local. |
| `seuilAcceleration` | 12 | Tours sans points avant accélération. |
| `seuilRarete` | 3 | Seuil de moules restantes pour accélérer. |
| `toursSansPointsMax` | 12 | Reset cible si stagnation. |
| `gainDistanceBonusMin` | 3 | Gain minimum pour consommer un bonus. |
| `gainDistanceBonusMinAccel` | 2 | Gain minimum en mode accél. |

## Groupes de réglages

### Vitesse vs score

- Augmente `penaliteDistance` pour gagner du temps.
- Augmente `valeurSaut` / `valeurTroisPas` pour ramasser des bonus plus vite.
- Baisse `coeffProfondeur` si tu veux un IA plus "greedy".

### Stabilité

- Augmente `penaliteBoucle` et `penaliteRetour` pour éviter les zigzags.
- Augmente `verrouillageCible` pour rester sur la même cible plus longtemps.

### Bonus

- `gainDistanceBonusMin` contrôle quand consommer un bonus.
- `penaliteUtilisationSaut` / `penaliteUtilisationTroisPas` évitent de gaspiller.

## Logs

- `log` : active un log simple.
- `logBrut=1` : ajoute le message brut du serveur.
- `logFichier=CHEMIN` : écrit un log à un endroit précis.

## Exemples

Profil rapide :

```bash
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe \
  penaliteDistance=1.9 coeffProfondeur=0.5
```

Profil plus "score" :

```bash
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe \
  valeurSaut=45 valeurTroisPas=70 penaliteDistance=1.2
```
