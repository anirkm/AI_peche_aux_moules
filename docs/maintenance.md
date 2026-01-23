# Maintenance

Cette page sert à maintenir le projet propre et corriger rapidement les bugs.

## Check‑list rapide avant commit

- [ ] Le serveur démarre sans erreur.
- [ ] Le client se connecte et joue un tour.
- [ ] Les logs ne sont pas vides.
- [ ] Aucun warning critique dans la console.
- [ ] Les seeds fixes donnent des résultats cohérents.

## Corriger un bug (méthode)

1. **Reproduire** le bug avec un seed fixe.
2. **Activer les logs** côté client.
3. **Comparer** l'action envoyée et l'état reçu.
4. **Isoler** la partie du code (parsing, mouvement, bonus).
5. **Corriger** + re‑tester sur plusieurs seeds.

## Problèmes fréquents et solutions

### L'IA tape dans les murs

- Vérifie le parsing de la structure (`Mu` vs `So`).
- Vérifie les coordonnées (x/y inversés).
- Contrôle la validité des mouvements (dans `MoteurSimulation`).

### L'IA boucle sans ramasser

- Augmente `penaliteBoucle` et `penaliteRetour`.
- Vérifie `verrouillageCible` et `seuilChangementCible`.
- Active `modePlan` si désactivé.

### Bonus utilisés trop tôt

- Augmente `gainDistanceBonusMin`.
- Augmente `penaliteUtilisationSaut` / `penaliteUtilisationTroisPas`.

### Résultats incohérents entre runs

- Fixe les seeds du serveur.
- Utilise `tools/seeds.txt` pour comparer.
- Compare les logs pour voir si la même action part au même tour.

## Scripts utiles

Nettoyage :

```bash
find IA -name "*.class" -delete
rm -rf IA/superAI/logs/*
```

Tests multi‑seeds :

```bash
AUTO_SERVER=1 ./tools/run_tests.sh 127.0.0.1 1337 MonEquipe tools/seeds.txt
```

## À vérifier après refacto

- Parsing du message serveur.
- Respect des règles bonus (saut / trois pas).
- Compatibilité des paramètres CLI.
