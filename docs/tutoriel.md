# Tutoriel "de 0 à partie"

Ce guide te prend par la main pour lancer une partie complète.

## 1) Préparer le projet

Place‑toi à la racine du repo :

```bash
pwd
ls
```

Tu dois voir `Serveur/`, `IA/`, `docs/`, `tools/`.

## 2) Compiler le serveur

```bash
cd Serveur
javac PecheAuxMoulesBoucle.java
```

Si tu as une erreur MG2D, vérifie l'installation (voir **Installation**).

## 3) Lancer le serveur (1 joueur)

```bash
java PecheAuxMoulesBoucle -nbJoueur 1 -delay 0 -timeout 3000
```

Tu dois voir :

- un log serveur,
- “Serveur en attente de connexion”.

## 4) Compiler l'IA

Ouvre un nouveau terminal et reviens à la racine, puis :

```bash
javac -d IA IA/superAI/*.java
```

## 5) Lancer l'IA

```bash
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe
```

Si tout va bien, le serveur affiche les tours et le score.

## 6) Activer un log client (recommandé)

```bash
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe \
  log logFichier=IA/superAI/logs/test.txt
```

Tu peux analyser ensuite :

```bash
python3 tools/analyse_logs.py IA/superAI/logs/test.txt
```

## 7) Lancer une partie reproductible

Pour comparer des IA, fixe les seeds :

```bash
java PecheAuxMoulesBoucle -nbJoueur 1 -delay 0 -timeout 3000 \
  -numLaby 49811 -numPlacementBonus 52567
```

## 8) Multi‑tests automatiques

```bash
AUTO_SERVER=1 ./tools/run_tests.sh 127.0.0.1 1337 MonEquipe tools/seeds.txt
```

## 9) Si ça bloque

- **Connection refused** : le serveur n'est pas lancé.
- **ClassNotFoundException** : recompile l'IA.
- **Logs vides** : ajoute `log` ou `logFichier=...`.

## 10) Résultat attendu

Tu dois obtenir :

- un score final,
- un fichier log client,
- un log serveur.

C'est bon : ta partie est complète.
