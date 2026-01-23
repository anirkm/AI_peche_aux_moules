# AI Pêche aux Moules

Projet de jeu + IA (client **superAI**) avec documentation utilisateur et technique.

## Auteurs

- Jeu / serveur : Rémi Synave
- IA / documentation : KARAMI Anir

## Le jeu (résumé détaillé)

L'IA pilote un joueur dans un **labyrinthe fermé** (murs tout autour). Le terrain est composé de :

- `Mu` : mur (infranchissable)
- `So` : sol (case vide)
- `Bs` : bonus saut (frites)
- `Bp` : bonus trois pas (bières)
- `Nombre` : moule (points)

Les coordonnées sont en **(x,y)** avec l'origine en haut‑gauche (x vers la droite, y vers le bas).
Les indices commencent à 0.

### Bonus

- **Bs (saut)** : avance de deux cases dans une direction, même si un mur est entre les deux.
  La case d'arrivée doit être marchable. Si elle ne l'est pas, l'action tente un pas simple.
- **Bp (trois pas)** : enchaîne trois déplacements dans le même tour.
  Les pas invalides sont ignorés, les pas valides sont exécutés.

### Multijoueur

Plusieurs IA peuvent jouer simultanément. Les joueurs peuvent se croiser ou partager une case.
La concurrence influence fortement la stratégie (cibles contestées).

### Fin de partie

La partie se termine quand :

- toutes les moules sont ramassées,
- la limite de tours est atteinte,
- tous les joueurs passent leur tour.

## Documentation

- En ligne : https://anirkm.github.io/AI_peche_aux_moules/
- Locale :

```bash
python3 -m pip install -r requirements.txt
mkdocs serve
```

## Démarrage rapide

Serveur (1 joueur) :

```bash
cd Serveur
javac PecheAuxMoulesBoucle.java
java PecheAuxMoulesBoucle -nbJoueur 1 -delay 0 -timeout 3000
```

Client IA :

```bash
javac -d IA IA/superAI/*.java
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe
```

## Lancer une partie reproductible (seeds fixes)

```bash
java PecheAuxMoulesBoucle -nbJoueur 1 -delay 0 -timeout 3000 \
  -numLaby 49811 -numPlacementBonus 52567
```

## Logs

Activer un log client :

```bash
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe \
  log logFichier=IA/superAI/logs/test.txt
```

Analyser :

```bash
python3 tools/analyse_logs.py IA/superAI/logs/test.txt
```

## Tests automatisés

```bash
AUTO_SERVER=1 ./tools/run_tests.sh 127.0.0.1 1337 MonEquipe tools/seeds.txt
```

## Dépendances

- Java JDK (8+ conseillé)
- MG2D (serveur) : https://github.com/synave/MG2D

## Licence

Voir `LICENSE`.
