# Protocole client/serveur

## Connexion

1. Ouvre une socket TCP vers l'IP/port du serveur.
2. Envoie le nom d'équipe (une ligne).
3. Reçois l'identifiant joueur (entier).
4. Boucle : reçois l'état, choisis un coup, renvoie la commande.

Le serveur envoie `FIN` quand la partie est terminée.

## Exemple d'échange minimal

```
Client -> "MonEquipe"
Serveur -> "0"
Serveur -> "13x11/....../2-1,8-11,2"
Client -> "E"
Serveur -> "13x11/....../2-2,8-11,2"
...
Serveur -> "FIN"
```

## Format du message

Le serveur envoie une chaîne :

```
largeurxhauteur/structure/infosJoueurs
```

Exemple :

```
13x11/Mu-Mu-Mu-...-Mu/2-1,8-11,2
```

### 1) Taille

- `13x11` signifie largeur=13, hauteur=11.

### 2) Structure

- Suite de tokens séparés par `-`.
- Tokens possibles : `Mu`, `So`, `Bs`, `Bp`, ou un nombre.
- Ordre : de gauche à droite, puis de haut en bas.
- Le nombre de tokens est **largeur * hauteur**.

### 3) Infos joueurs

Format :

```
N-x0,y0-x1,y1-...
```

- `N` = nombre de joueurs.
- Chaque `x,y` est une position.

!!! note
    Le sujet indique les positions de départ : joueur 0 en haut à gauche, joueur 1 en bas à droite,
    joueur 2 en haut à droite, joueur 3 en bas à gauche.

## Exemple de parsing (pseudo)

```text
parts = message.split("/")
size = parts[0]
structure = parts[1]
players = parts[2]

width,height = size.split("x")
tokens = structure.split("-")
assert len(tokens) == width*height

nb = players.split("-")[0]
coords = players.split("-")[1:]
```

## Commandes à envoyer

- Mouvements simples : `N`, `S`, `E`, `O`, `C`.
- Bonus saut : `Bs-DIRECTION` (ex: `Bs-S`).
- Bonus trois pas : `Bp-D1-D2-D3` (ex: `Bp-E-E-S`).

### Règles importantes

- Si un mouvement est invalide (mur / hors limite), il est ignoré.
- Le saut ne consomme le bonus que si la case à +2 est valide.
- Le bonus 3 pas exécute chaque pas valide, et ignore les autres.
