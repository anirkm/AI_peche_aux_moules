# Installation

## Prérequis

- Java JDK (8+ conseillé).
- MG2D pour l'interface graphique du serveur.

Vérifie Java :

```bash
java -version
javac -version
```

## Installer MG2D

1. Télécharge MG2D : https://github.com/synave/MG2D
2. Place le dossier `MG2D` dans `Serveur/MG2D`.
3. Si tu veux le garder ailleurs, adapte le classpath au moment de compiler/lancer.

!!! note
    Sans MG2D, le serveur peut ne pas s'afficher ou refuser de lancer l'interface.

## Compiler le serveur

=== "Linux/macOS"

    ```bash
    cd Serveur
    javac PecheAuxMoulesBoucle.java
    ```

=== "Windows"

    ```bat
    cd Serveur
    javac PecheAuxMoulesBoucle.java
    ```

Si MG2D n'est pas dans `Serveur/MG2D`, ajoute le classpath :

=== "Linux/macOS"

    ```bash
    javac -cp .:chemin/MG2D PecheAuxMoulesBoucle.java
    ```

=== "Windows"

    ```bat
    javac -cp .;chemin\MG2D PecheAuxMoulesBoucle.java
    ```

## Lancer le serveur

```bash
java PecheAuxMoulesBoucle -nbJoueur 1 -delay 0 -timeout 3000
```

Options utiles :

```bash
java PecheAuxMoulesBoucle -h
java PecheAuxMoulesBoucle -nbJoueur 2 -delay 0 -timeout 3000 -numLaby 49811 -numPlacementBonus 52567
```

## Compiler l'IA

```bash
javac -d IA IA/superAI/*.java
```

## Lancer l'IA

```bash
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe
```

Avec logs :

```bash
java -cp IA superAI.ClientSuperAI 127.0.0.1 1337 MonEquipe \
  log logFichier=IA/superAI/logs/test.txt
```

## Aide intégrée

```bash
java -cp IA superAI.ClientSuperAI -h
```

## Sur Windows

- Utilise `;` dans le classpath.
- Si `java` n'est pas reconnu, ajoute le JDK au PATH.

## Erreurs courantes

- **ClassNotFoundException** : vérifie le `-cp IA` et la compilation.
- **Connection refused** : le serveur n'est pas lancé ou le port est mauvais.
- **MG2D manquant** : place `MG2D` dans `Serveur/MG2D`.
