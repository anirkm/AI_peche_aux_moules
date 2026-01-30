# Guide joueur

Ce guide explique le jeu sans entrer dans le code.

## Objectif

Marquer le plus de points en ramassant les moules avant la fin de la partie.

## Vocabulaire rapide

- **Moule** : case qui donne des points.
- **Bonus saut (Bs)** : avance de 2 cases (même si un mur est au milieu).
- **Bonus trois pas (Bp)** : enchaîne 3 mouvements en un tour.
- **Tour** : un aller/retour serveur <-> client.
- **Case** : un bloc du labyrinthe.

## Déroulement d'une partie

1. Le serveur attend les joueurs.
2. Chaque joueur envoie son nom d'équipe.
3. Le serveur annonce l'identifiant du joueur.
4. À chaque tour, le serveur envoie l'état du labyrinthe.
5. Le joueur renvoie un coup.
6. La partie s'arrête quand les moules sont finies ou que la limite de tours est atteinte.

## Déplacements de base

- Nord = `N`, Sud = `S`, Est = `E`, Ouest = `O`.
- `C` = passer son tour.
- Un mur (`Mu`) bloque le déplacement.

## Bonus

### Saut (Bs)

- Commande : `Bs-DIRECTION` (ex: `Bs-S`).
- Avance de 2 cases.
- Le mur intermédiaire est ignoré.
- Si la case à +2 est invalide, le joueur tente un pas simple.
- Les bonus et moules sur le trajet sont ramassés.

### Trois pas (Bp)

- Commande : `Bp-D1-D2-D3` (ex: `Bp-E-E-S`).
- Les pas invalides sont ignorés.
- Les bonus et moules sur le trajet sont ramassés.

## Score et priorités

- Les moules ont une valeur (points) décidée au début.
- Les bonus ne donnent pas de points directement.
- La vitesse compte souvent plus que la trajectoire parfaite.

## Conseils joueur

- Évite les demi-tours inutiles.
- Utilise les bonus quand ils font gagner plusieurs tours.
- Si une zone est vide, change de direction.
- En multi‑joueurs, anticipe la concurrence (cibles proches des autres joueurs).

## Erreurs courantes

- Envoyer un coup invalide (mur, hors limite) = tour perdu.
- Consommer un bonus pour un gain nul = gaspillage.

!!! tip
    Si tu hésites entre deux chemins, choisis celui avec le plus de moules visibles :
    la vitesse prime si tu ramasses au passage.
