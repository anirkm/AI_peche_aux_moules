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

### Ramassage

- Ramasser une moule ajoute immédiatement ses points.
- Ramasser un bonus l’ajoute à l’inventaire.
- La case ramassée devient du sol (`So`).

### Bonus

- **Bs (saut)** : avance de deux cases dans une direction, même si un mur est entre les deux.
  La case d'arrivée doit être marchable. Si elle ne l'est pas, l'action tente un pas simple.
- **Bp (trois pas)** : enchaîne trois déplacements dans le même tour.
  Les pas invalides sont ignorés, les pas valides sont exécutés.
  Les directions invalides sont traitées comme `C`.

#### Cas limites (serveur)

- Déplacement vers un mur = refusé (le joueur reste sur place).
- Bs est consommé **uniquement** si la case d’arrivée est valide (sol/bonus/moule).
- Si la case d’arrivée de Bs est un mur, un pas simple est tenté.
- Si Bp contient une direction invalide, elle devient `C`.
- Bp consomme le bonus si le joueur en possède un.

### Multijoueur

Plusieurs IA peuvent jouer simultanément. Les joueurs peuvent se croiser ou partager une case.
La concurrence influence fortement la stratégie (cibles contestées).

### Fin de partie

La partie se termine quand :

- toutes les moules sont ramassées,
- la limite de tours est atteinte,
- tous les joueurs passent leur tour.

## Technique IA (résumé)

L'IA fait un scoring local : on calcule des distances exactes, puis on choisit l'action
qui maximise un score (points + futur proche), tout en évitant les boucles.

### 1) Distances exactes (BFS)

- **Parcours en largeur (BFS)** : calcule la distance minimale en nombre de tours vers
  toutes les cases (coût uniforme = 1 par tour).
- **BFS multi‑états** : pour tenir compte des bonus, l'état devient :
  `(case, nbSautRestant, nbTroisPasRestant)`.  
  On obtient ainsi une distance réaliste quand on consomme un saut ou un trois‑pas.

Justification : le BFS est optimal sur un graphe non pondéré (coût par action = 1 tour).
Quand on ajoute les bonus, on étend l'espace d'états ; chaque transition coûte encore 1,
donc le BFS reste optimal et fournit **la distance minimale en tours**.

### 2) Évaluation des actions

Pour chaque action possible, on simule localement puis on calcule un score :

```
score = points_gagnés
      + valeurSaut * bonusSaut_gagné
      + valeurTroisPas * bonusTroisPas_gagné
      - penaliteUtilisationSaut * bonusSaut_utilisé
      - penaliteUtilisationTroisPas * bonusTroisPas_utilisé
      + futur
```

Le terme **futur** correspond à la meilleure cible restante, pondérée par :
la distance, la valeur et la concurrence (si un adversaire est plus proche).

Mathématiquement, on approxime la valeur d'une cible `c` par :

```
V(c) = valeur(c) * contest(d_moi, d_adv) - penaliteDistance * d_moi
```

où `contest` diminue si un adversaire est plus proche (ex: 1, 0.5, 0.25, 0).
Cette pondération représente une **espérance de gain** sous concurrence.

### 3) Mémoire & anti‑boucle

Pour éviter les zigzags :

- **Mémoire de positions** : pénalise les retours récents.
- **Verrouillage de cible** : on garde une cible quelques tours avant de changer.
- **Pénalités** : demi‑tours, cycles courts, immobilité.

### 4) Lookahead court + plan local

- **Lookahead** : on regarde un coup d'avance (profondeur 2) pour limiter le côté “greedy”.
- **Plan local Top‑K** : on teste une petite séquence de cibles (top‑K) et on choisit
  la première cible de la meilleure séquence.

Justification : un lookahead court réduit les erreurs myopes sans exploser le temps de calcul.
Le Top‑K limite la combinatoire : on approxime la recherche exhaustive par un sous‑ensemble
des cibles les plus prometteuses.

### 5) Mode hybride (vitesse vs score)

Si l'IA stagne ou s'il reste très peu de moules, elle privilégie la **distance**
pour terminer plus vite. Les bonus ne sont consommés que si le gain en tours
est suffisant.

Cette approche donne un bon compromis entre **performance**, **qualité de décision**
et **temps de calcul**.

### 6) Organisation du code IA

- **IA.java** : orchestration générale.
- **IAActions.java** : actions + scoring + pénalités.
- **IACibles.java** : choix de cibles + adversaires.
- **IABeam.java** : faisceau (optionnel).

Par défaut, les modules optionnels (adversaires / carte de valeur / bonus avancés)
sont **désactivés** pour rester rapide (preset G1B0).

### Optimisations légères (sans impact logique)

- Cache BFS intra‑tour : même source = même distance (évite des recalculs).
- Liste compacte des cases utiles (moules/bonus) pour accélérer l’évaluation du futur.

### Presets disponibles

- **G1B0** : preset par défaut, rapide et stable.
- **G1B0P** : active les modules optionnels (adversaires / carte de valeur / bonus avancés).
- **G1AUTO** : beam auto (active le faisceau seulement sur cartes complexes).

### Beam auto (modeBeam=2)

Le faisceau s’active uniquement si la carte paraît complexe :

- ratio de murs élevé,
- beaucoup de moules restantes,
- grande taille de grille.

### Paramètres IA clés (extraits)

- `modePlan`, `nbCiblesPlan`, `profondeurPlan` : planification locale top‑K.
- `modeBeam`, `profondeurBeam`, `largeurBeam` : faisceau (coût vs stabilité).
- `penaliteRepet`, `penaliteAllerRetour` : anti‑boucles.
- `gainDistanceBonusMin`, `gainDistanceBonusMinAccel` : usage des bonus.
- `penaliteCibleAdverse`, `coeffCarteValeur`, `bonusUsageEfficace` : modules optionnels.

## Documentation

- En ligne : https://anirkm.github.io/AI_peche_aux_moules/
- Locale :

```bash
python3 -m pip install -r requirements.txt
mkdocs serve
```

## Manuels PDF

Deux manuels sont fournis :

- `manuel_utilisateur.pdf`
- `manuel_administrateur.pdf`

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

Le port par défaut est **1337**.

### Options serveur utiles

- `-nbJoueur` : nombre de joueurs
- `-delay` : délai entre tours (ms)
- `-timeout` : durée max d’une partie (ms)
- `-numLaby` : seed labyrinthe
- `-numPlacementBonus` : seed bonus
- `-tauxDeMur` : densité de murs (0–50)

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

Fichiers utiles :

- `IA/superAI/logs/` : logs clients
- `tools/results.csv` : agrégat des tests

## Tests automatisés

```bash
AUTO_SERVER=1 ./tools/run_tests.sh 127.0.0.1 1337 MonEquipe tools/seeds.txt
```

## Dépendances

- Java JDK (8+ conseillé)
- MG2D (serveur) : https://github.com/synave/MG2D

## Licence

Voir `LICENSE`.
