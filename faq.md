# FAQ

Voici les questions les plus fréquentes, avec des réponses courtes.

<details>
<summary><strong>Le client affiche "Utilisation" quand je lance</strong></summary>

Tu n'as pas passé assez d'arguments. Lance :

```bash
java -cp IA superAI.ClientSuperAI -h
```
</details>

<details>
<summary><strong>ClassNotFoundException: superAI.ClientSuperAI</strong></summary>

Tu n'as pas compilé ou le classpath est mauvais :

```bash
javac -d IA IA/superAI/*.java
java -cp IA superAI.ClientSuperAI ...
```
</details>

<details>
<summary><strong>Connection refused</strong></summary>

Le serveur n'est pas lancé ou le port est différent. Vérifie `-nbJoueur` et le port (1337).
</details>

<details>
<summary><strong>L'IA envoie un saut sur un mur</strong></summary>

Le saut ne consomme le bonus que si la case à +2 est valide. Sinon, il revient à un pas simple.
</details>

<details>
<summary><strong>Les résultats changent d'un seed à l'autre</strong></summary>

C'est normal. Utilise `tools/seeds.txt` pour comparer.
</details>

<details>
<summary><strong>Je veux comparer rapidement des presets</strong></summary>

```bash
AUTO_SERVER=1 ./tools/run_tests.sh 127.0.0.1 1337 MonEquipe tools/seeds.txt
```
</details>

<details>
<summary><strong>GitHub Pages ne s'affiche pas</strong></summary>

Vérifie que l'action GitHub a tourné et que la branche `gh-pages` existe.
</details>

<details>
<summary><strong>Les logs sont vides</strong></summary>

Ajoute `log` ou `logFichier=...` aux options du client.
</details>
