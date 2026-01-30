# Règles du jeu

Cette page reprend les règles officielles du sujet et leur interprétation côté client.

## Plateau

- Le labyrinthe est un rectangle entouré de murs (aucune sortie possible).
- Les cases sont indexées en (x,y) avec l'origine en haut gauche.
- On lit de gauche à droite puis de haut en bas.
- Plusieurs joueurs peuvent se retrouver sur une même case.
 - Les indices commencent à 0.

## Types de cases

- `Mu` : mur (infranchissable).
- `So` : sol (case vide).
- `Bs` : bonus saut (frites).
- `Bp` : bonus trois pas (bières).
- `Nombre` : moule (points).

Les cases `Bs`, `Bp` et les moules disparaissent après passage (deviennent `So`).

## Déplacements simples

- `N` : nord, `S` : sud, `E` : est, `O` : ouest.
- `C` : rester sur place (passer son tour).
- Un mur bloque le mouvement.

## Bonus saut (Bs)

- Commande : `Bs-DIRECTION`.
- Avance de deux cases dans la direction.
- Le mur intermédiaire est ignoré.
- **Condition** : la case d'arrivée doit être marchable (sol, bonus, moule).
- Si la case d'arrivée est invalide, le joueur tente un pas simple.
- Les bonus/moules sur le trajet sont ramassés.

## Bonus trois pas (Bp)

- Commande : `Bp-D1-D2-D3`.
- Permet d'enchaîner trois déplacements.
- Chaque pas est valide ou ignoré, indépendamment des autres.
- Les bonus/moules sur le trajet sont ramassés.

## Score

- Chaque moule donne un nombre de points (valeur fixée en début de partie).
- Les bonus ne donnent pas de points directs, mais accélèrent les tours.

## Fin de partie

- Toutes les moules sont ramassées.
- Le nombre max de tours est atteint.
- Tous les joueurs passent leur tour.

![Exemple de labyrinthe](assets/images/labyExempleCode.png)
