# Lancer une partie

## Mode simple (1 joueur)

1) Lancer le serveur :

```bash
cd Serveur
javac PecheAuxMoulesBoucle.java
java PecheAuxMoulesBoucle -nbJoueur 1 -delay 0 -timeout 3000
```

2) Lancer l'IA :

```bash
javac -d IA IA/superAI/*.java
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe
```

## Mode multi‑joueurs

Lance le serveur avec `-nbJoueur 2` (ou plus) puis connecte chaque client :

```bash
java PecheAuxMoulesBoucle -nbJoueur 2 -delay 0 -timeout 3000
```

Chaque client doit utiliser le même IP/port.

## Reproductibilité (seeds fixes)

Pour comparer des IA, fixe le labyrinthe :

```bash
java PecheAuxMoulesBoucle -nbJoueur 1 -delay 0 -timeout 3000 \
  -numLaby 49811 -numPlacementBonus 52567
```

## Lancer sans interface

Si l'interface graphique pose problème, testez avec un serveur en boucle et un timeout court :

```bash
java PecheAuxMoulesBoucle -nbJoueur 1 -delay 0 -timeout 2000
```

## Logs utiles

Côté client :

```bash
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe \
  log logFichier=IA/superAI/logs/test.txt
```

Côté serveur :

- Le serveur écrit un fichier `logServeurLaby-YYYYMMDD-HHMMSS`.
- Il indique les mouvements reçus et les erreurs.

## Astuces pratiques

- Pour comparer des presets, garde les mêmes seeds.
- Lancer plusieurs clients en parallèle augmente la charge CPU.
- Si un client bloque, coupe‑le et relance avec logs.
