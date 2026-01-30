# Dépannage

## Impossible de lancer le serveur

- Vérifie Java : `java -version`.
- MG2D doit être dans `Serveur/MG2D`.
- Recompile : `javac PecheAuxMoulesBoucle.java`.

## Client ne trouve pas la classe

Erreur : `ClassNotFoundException: superAI.ClientSuperAI`

```bash
javac -d IA IA/superAI/*.java
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe
```

## Connection refused

- Le serveur n'est pas lancé.
- Mauvais port / IP.
- Le serveur a crash.

!!! warning
    Vérifie que le serveur écoute bien sur `1337` et que rien d'autre n'utilise ce port.

## L'IA bouge dans le vide

- Vérifie le parsing (taille/structure/infos joueurs).
- Assure‑toi que l'indexation (x,y) est correcte.
- Vérifie que le nombre de tokens = largeur * hauteur.

## Erreurs de compilation Java

- `package ... does not match` : dossier / package incohérent.
- `string literal not properly closed` : guillemets manquants.
- `type ... is already defined` : doublon de classe dans le classpath.

## Le test multi‑seeds rate

- Utilise `AUTO_SERVER=1` pour auto‑lancer le serveur.
- Vérifie que `tools/seeds.txt` a des valeurs correctes.
- Si besoin, vide `IA/superAI/logs/`.

## Nettoyage rapide

```bash
find IA -name "*.class" -delete
rm -rf IA/superAI/logs/*
```

## Logs serveur illisibles

Si le serveur indique un message illisible :

- vérifie que l'action envoyée respecte le format,
- évite les espaces dans les commandes,
- logge côté client pour voir la commande exacte.
