package superAI;

/**
 * Regroupe la génération d'actions et l'évaluation des coups.
 */
class IAActions {
    // --- Actions possibles ---
    static Action[] actionsPossibles(Labyrinthe laby, Joueur joueur, Inventaire inventaire) {
        // Génère toutes les actions légales depuis la position du joueur
        // Taille max : 1 (C) + 4 (simples) + 4 (sauts) + 64 (3 pas)
        int max = 1 + 4 + 4 + 64;
        Action[] actions = new Action[max];
        int idx = 0;
        // Bonus dispo
        int saut = inventaire.getBonusSaut() > 0 ? 4 : 0;
        int trois = inventaire.getBonusTroisPas() > 0 ? 64 : 0;

        char[] simples = new char[]{'N', 'S', 'E', 'O'};
        // Déplacements simples
        for (char dir : simples) {
            // On ne conserve que les pas qui sont réellement jouables
            if (deplacementValide(laby, joueur.getX(), joueur.getY(), dir)) {
                actions[idx++] = Action.simple(dir);
            }
        }

        if (saut > 0) {
            // On n'ajoute que les sauts dont la case d'arrivée est valide
            for (char dir : simples) {
                if (sautValide(laby, joueur.getX(), joueur.getY(), dir)) {
                    actions[idx++] = Action.saut(dir);
                }
            }
        }

        if (trois > 0) {
            // 4^3 combinaisons ; le serveur ignore les pas invalides
            // On garde toutes les combinaisons, la simulation appliquera la règle serveur.
            char[] dirs = new char[]{'N', 'S', 'E', 'O'};
            // Toutes les suites possibles de 3 pas
            for (char d1 : dirs) {
                for (char d2 : dirs) {
                    for (char d3 : dirs) {
                        actions[idx++] = Action.troisPas(d1, d2, d3);
                    }
                }
            }
        }

        if (idx == 0) {
            // Aucun mouvement légal
            // On laisse "C" comme fallback minimal pour éviter une erreur
            return new Action[]{Action.simple('C')};
        }
        // Coupe du tableau pour ne garder que les actions réelles
        Action[] coupe = new Action[idx];
        System.arraycopy(actions, 0, coupe, 0, idx);
        return coupe;
    }

    // --- Evaluation des actions ---
    static void enregistrerTop(EvaluationAction[] top, Action action, double score, ResultatSimulation sim) {
        // Insère dans un top-3 simple
        // Utile uniquement pour le logging : on garde les 3 meilleures actions vues
        EvaluationAction eval = new EvaluationAction(action, score, sim);

        if (top[0] == null || score > top[0].score) {
            top[2] = top[1];
            top[1] = top[0];
            top[0] = eval;
            return;
        }
        if (top[1] == null || score > top[1].score) {
            top[2] = top[1];
            top[1] = eval;
            return;
        }
        if (top[2] == null || score > top[2].score) {
            top[2] = eval;
        }
    }

    static double meilleurApres(EtatJeu etat, int idJoueur, ResultatSimulation sim1,
                                Inventaire inventaire, int[] distAdversaires, int[] casesInteret,
                                ParametresIA parametres, int[][] bfsCache) {
        // Évalue un coup d'avance (profondeur 2)
        // Ce lookahead reste volontairement court pour garder un coût faible.
        Labyrinthe laby = etat.getLabyrinthe();
        // Joueur "virtuel" sur la nouvelle position
        Joueur joueur = new Joueur(idJoueur, sim1.x, sim1.y);
        double meilleur = -1e18;

        for (Action action : actionsPossibles(laby, joueur, inventaire)) {
            // On simule un 2e coup
            // sim1 est passé en "dejaCollecte" pour éviter un double comptage
            ResultatSimulation sim2 = MoteurSimulation.appliquer(etat, joueur, inventaire, action, sim1);
            int[] dist = Distances.bfsCached(laby, sim2.x, sim2.y, bfsCache);
            double score = scoreSimulation(laby, dist, distAdversaires, sim2, sim1, casesInteret, parametres);
            if (score > meilleur) {
                meilleur = score;
            }
        }

        return meilleur == -1e18 ? 0.0 : meilleur;
    }

    static double scoreAction(Labyrinthe laby, ResultatSimulation sim, ResultatSimulation deja,
                              int[] dist, int[] distAdversaires, int[] ciblesAdversaires,
                              int[] casesInteret, Action action, MemoirePositions memoire,
                              char dernierMouvement, Inventaire inventaireAvant, int departX, int departY,
                              int cibleReference, ParametresIA parametres, boolean acceleration) {
        // Pipeline de scoring : score brut + pénalités + gestion fine des bonus
        double score = scoreSimulation(laby, dist, distAdversaires, sim, deja, casesInteret, parametres);
        score = IACibles.appliquerPenaliteAdversaires(score, laby, sim, dist, distAdversaires, ciblesAdversaires, parametres);
        score = appliquerPenalites(score, action, sim, laby, memoire, dernierMouvement, parametres);
        score = penaliserBonusPeuRentable(score, action, sim, laby, departX, departY,
            inventaireAvant, cibleReference, parametres, acceleration);
        return score;
    }

    private static double scoreSimulation(Labyrinthe laby, int[] dist,
                                          int[] distAdversaires, ResultatSimulation sim,
                                          ResultatSimulation deja, int[] casesInteret,
                                          ParametresIA parametres) {
        // Score instantané + estimation du futur proche
        // Formule globale : points immédiats + valeur des bonus - coût d'usage + futur
        double futur = evaluerFutur(laby, dist, distAdversaires, sim, deja, casesInteret, parametres);
        // Champ de valeur global pour tirer vers les zones denses
        futur += evaluerCarteValeur(laby, dist, distAdversaires, casesInteret, parametres);
        // Ajout des points directs et des bonus
        return sim.points
            + parametres.valeurSaut * sim.bonusSautGagne
            + parametres.valeurTroisPas * sim.bonusTroisPasGagne
            - parametres.penaliteUtilisationSaut * sim.bonusSautUtilise
            - parametres.penaliteUtilisationTroisPas * sim.bonusTroisPasUtilise
            + futur;
    }

    private static double penaliserBonusPeuRentable(double score, Action action, ResultatSimulation sim,
                                                    Labyrinthe laby, int departX, int departY,
                                                    Inventaire inventaireAvant, int cibleReference,
                                                    ParametresIA parametres, boolean acceleration) {
        // Bonus trop "cher" si le gain de distance est faible
        // On compare la distance vers la cible avant/après la consommation du bonus.
        if (cibleReference == -1) {
            return score;
        }
        if (action.getType() == Action.Type.SIMPLE) {
            return score;
        }
        if (sim.bonusSautUtilise == 0 && sim.bonusTroisPasUtilise == 0) {
            return score;
        }

        int dAvant = Distances.distanceAvecBonus(laby, departX, departY,
            inventaireAvant.getBonusSaut(), inventaireAvant.getBonusTroisPas(), cibleReference);
        if (dAvant >= Distances.INFINI) {
            return score;
        }

        Inventaire invApres = inventaireAvant.copie();
        invApres.appliquer(sim);
        int dApres = Distances.distanceAvecBonus(laby, sim.x, sim.y,
            invApres.getBonusSaut(), invApres.getBonusTroisPas(), cibleReference);
        if (dApres >= Distances.INFINI) {
            return score;
        }

        int gain = dAvant - dApres;
        int minGain = acceleration ? parametres.gainDistanceBonusMinAccel : parametres.gainDistanceBonusMin;
        if (gain < minGain) {
            // Pénalité supplémentaire si le bonus n'améliore pas assez la route
            if (sim.bonusSautUtilise > 0) {
                score -= parametres.penaliteUtilisationSaut;
            }
            if (sim.bonusTroisPasUtilise > 0) {
                score -= parametres.penaliteUtilisationTroisPas;
            }
        } else if (parametres.bonusUsageEfficace > 0.0) {
            // Récompense si le bonus apporte un vrai gain
            score += (gain - minGain + 1) * parametres.bonusUsageEfficace;
        }

        return score;
    }

    private static double evaluerFutur(Labyrinthe laby, int[] dist,
                                       int[] distAdversaires, ResultatSimulation sim,
                                       ResultatSimulation deja, int[] casesInteret,
                                       ParametresIA parametres) {
        // Cherche la meilleure case "rentable" à moyen terme
        // Si on a une liste de cases utiles, on l'utilise pour éviter
        // de balayer toute la grille.
        double meilleur = -1e18;

        if (casesInteret == null) {
            for (int y = 0; y < laby.getHauteur(); y++) {
                for (int x = 0; x < laby.getLargeur(); x++) {
                    int idx = laby.index(x, y);
                    if (estCollecte(sim, deja, idx)) {
                        continue;
                    }
                    double base = IACibles.valeurCase(laby.getCase(x, y), parametres, false);
                    if (base < 0.0) {
                        continue;
                    }
                    int d = dist[idx];
                    if (d >= Distances.INFINI) {
                        continue;
                    }
                    int dOpp = distAdversaires[idx];
                    double contest = IACibles.estimationContest(d, dOpp);
                    double score = base * contest - parametres.penaliteDistance * d;
                    if (score > meilleur) {
                        meilleur = score;
                    }
                }
            }
        } else {
            int width = laby.getLargeur();
            for (int i = 0; i < casesInteret.length; i++) {
                int idx = casesInteret[i];
                if (estCollecte(sim, deja, idx)) {
                    continue;
                }
                int x = idx % width;
                int y = idx / width;
                double base = IACibles.valeurCase(laby.getCase(x, y), parametres, false);
                if (base < 0.0) {
                    continue;
                }
                int d = dist[idx];
                if (d >= Distances.INFINI) {
                    continue;
                }
                int dOpp = distAdversaires[idx];
                double contest = IACibles.estimationContest(d, dOpp);
                double score = base * contest - parametres.penaliteDistance * d;
                if (score > meilleur) {
                    meilleur = score;
                }
            }
        }

        return meilleur == -1e18 ? 0.0 : meilleur;
    }

    private static double evaluerCarteValeur(Labyrinthe laby, int[] dist,
                                             int[] distAdversaires, int[] casesInteret,
                                             ParametresIA parametres) {
        // Somme pondérée sur la grille pour créer un champ de potentiel
        // Permet d'attirer l'IA vers des zones denses en ressources.
        double coeff = parametres.coeffCarteValeur;
        if (coeff <= 0.0) {
            return 0.0;
        }
        double total = 0.0;
        double rMurs = IACibles.ratioMurs(laby);
        if (rMurs > 0.35) {
            coeff *= 1.0 + (rMurs - 0.35) * 2.0;
        }
        coeff = Math.min(coeff, parametres.coeffCarteValeur * 2.5);
        if (casesInteret == null) {
            for (int y = 0; y < laby.getHauteur(); y++) {
                for (int x = 0; x < laby.getLargeur(); x++) {
                    int idx = laby.index(x, y);
                    int d = dist[idx];
                    if (d >= Distances.INFINI) {
                        continue;
                    }
                    double base = IACibles.valeurCase(laby.getCase(x, y), parametres, false);
                    if (base < 0.0) {
                        continue;
                    }
                    int dOpp = distAdversaires[idx];
                    double contest = IACibles.estimationContest(d, dOpp);
                    double poids = 1.0 / (1.0 + d);
                    total += base * contest * poids * coeff;
                }
            }
        } else {
            int width = laby.getLargeur();
            for (int i = 0; i < casesInteret.length; i++) {
                int idx = casesInteret[i];
                int d = dist[idx];
                if (d >= Distances.INFINI) {
                    continue;
                }
                int x = idx % width;
                int y = idx / width;
                double base = IACibles.valeurCase(laby.getCase(x, y), parametres, false);
                if (base < 0.0) {
                    continue;
                }
                int dOpp = distAdversaires[idx];
                double contest = IACibles.estimationContest(d, dOpp);
                double poids = 1.0 / (1.0 + d);
                total += base * contest * poids * coeff;
            }
        }
        return total;
    }

    // --- Penalites et utilitaires ---
    private static double appliquerPenalites(double score, Action action, ResultatSimulation sim,
                                             Labyrinthe laby, MemoirePositions memoire,
                                             char dernierMouvement, ParametresIA parametres) {
        // Pénalités anti-boucle, demi-tour et immobilité
        // On pénalise fortement les oscillations pour éviter le va-et-vient.
        if (estRetour(action, dernierMouvement)) {
            score -= parametres.penaliteRetour;
        }
        if (memoire != null) {
            int idx = laby.index(sim.x, sim.y);
            int freq = memoire.compter(idx);
            if (freq > 0) {
                score -= parametres.penaliteBoucle * freq * freq;
            }
            // Évite les oscillations A <-> B
            int avant = memoire.recent(1);
            if (avant != -1 && idx == avant) {
                score -= parametres.penaliteRetour * 2.0;
            }
        }
        if (!sim.aBouge() && sim.points == 0 && sim.bonusSautGagne == 0 && sim.bonusTroisPasGagne == 0) {
            // Rien de gagné et aucune case franchie
            score -= parametres.penaliteImmobile;
            if (action.getType() == Action.Type.SIMPLE && action.getD1() == 'C') {
                score -= parametres.penaliteImmobile;
            }
        }
        return score;
    }

    private static boolean estRetour(Action action, char dernierMouvement) {
        // Détecte un demi-tour immédiat
        if (dernierMouvement == 'C') {
            return false;
        }
        char d = action.dernierMouvement();
        if (d == 'C') {
            return false;
        }
        return opposer(dernierMouvement) == d;
    }

    private static char opposer(char dir) {
        // Direction inverse
        switch (dir) {
            case 'N':
                return 'S';
            case 'S':
                return 'N';
            case 'E':
                return 'O';
            case 'O':
                return 'E';
            default:
                return 'C';
        }
    }

    private static boolean estCollecte(ResultatSimulation sim, ResultatSimulation deja, int idx) {
        // Évite de re-compter une case déjà prise dans la simulation
        if (sim != null && sim.aCollecte(idx)) {
            return true;
        }
        return deja != null && deja.aCollecte(idx);
    }

    private static boolean deplacementValide(Labyrinthe laby, int x, int y, char dir) {
        // Mouvement simple
        // On ne valide que si la case d'arrivée est dans le labyrinthe et non mur.
        int[] d = delta(dir);
        int nx = x + d[0];
        int ny = y + d[1];
        return laby.dansBornes(nx, ny) && !laby.estMur(nx, ny);
    }

    private static boolean sautValide(Labyrinthe laby, int x, int y, char dir) {
        // Saut : 2 cases d'un coup
        // La case d'arrivée doit être marchable (règle serveur).
        int[] d = delta(dir);
        int nx = x + d[0] * 2;
        int ny = y + d[1] * 2;
        return laby.dansBornes(nx, ny) && !laby.estMur(nx, ny);
    }

    private static int[] delta(char dir) {
        // Vecteur (dx, dy)
        switch (dir) {
            case 'N':
                return new int[]{0, -1};
            case 'S':
                return new int[]{0, 1};
            case 'E':
                return new int[]{1, 0};
            case 'O':
                return new int[]{-1, 0};
            default:
                return new int[]{0, 0};
        }
    }
}
