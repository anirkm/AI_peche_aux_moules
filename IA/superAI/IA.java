package superAI;

class IA {
    private static final double VALEUR_SAUT = 20.0;
    private static final double VALEUR_TROIS_PAS = 30.0;
    private static final double PENALITE_UTILISATION_SAUT = 8.0;
    private static final double PENALITE_UTILISATION_TROIS_PAS = 12.0;
    private static final double PENALITE_DISTANCE = 1.4;

    static Action choisirAction(EtatJeu etat, int idJoueur, Inventaire inventaire) {
        Labyrinthe laby = etat.getLabyrinthe();
        Joueur joueur = etat.getJoueur(idJoueur);

        // Distance minimale des adversaires vers chaque case
        int[] distAdversaires = distancesAdversaires(etat, idJoueur);

        Action meilleure = Action.simple('C');
        double meilleurScore = -1e18;

        for (Action action : actionsPossibles(inventaire)) {
            ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
            int[] dist = Distances.bfs(laby, sim.x, sim.y);

            double futur = evaluerFutur(laby, dist, distAdversaires, sim);
            double score = sim.points
                + VALEUR_SAUT * sim.bonusSautGagne
                + VALEUR_TROIS_PAS * sim.bonusTroisPasGagne
                - PENALITE_UTILISATION_SAUT * sim.bonusSautUtilise
                - PENALITE_UTILISATION_TROIS_PAS * sim.bonusTroisPasUtilise
                + futur;

            if (score > meilleurScore) {
                meilleurScore = score;
                meilleure = action;
            }
        }

        return meilleure;
    }

    private static Action[] actionsPossibles(Inventaire inventaire) {
        int base = 5;
        int saut = inventaire.getBonusSaut() > 0 ? 4 : 0;
        int trois = inventaire.getBonusTroisPas() > 0 ? 64 : 0;
        Action[] actions = new Action[base + saut + trois];
        int idx = 0;

        actions[idx++] = Action.simple('N');
        actions[idx++] = Action.simple('S');
        actions[idx++] = Action.simple('E');
        actions[idx++] = Action.simple('O');
        actions[idx++] = Action.simple('C');

        if (saut > 0) {
            actions[idx++] = Action.saut('N');
            actions[idx++] = Action.saut('S');
            actions[idx++] = Action.saut('E');
            actions[idx++] = Action.saut('O');
        }

        if (trois > 0) {
            char[] dirs = new char[]{'N', 'S', 'E', 'O'};
            for (char d1 : dirs) {
                for (char d2 : dirs) {
                    for (char d3 : dirs) {
                        actions[idx++] = Action.troisPas(d1, d2, d3);
                    }
                }
            }
        }

        return actions;
    }

    private static double evaluerFutur(Labyrinthe laby, int[] dist,
                                       int[] distAdversaires, ResultatSimulation sim) {
        double meilleur = -1e18;

        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                int idx = laby.index(x, y);
                if (sim.aCollecte(idx)) {
                    continue;
                }

                CaseJeu c = laby.getCase(x, y);
                double base;
                if (c.getType() == CaseJeu.Type.MOULE) {
                    base = c.getValeur();
                } else if (c.getType() == CaseJeu.Type.SAUT) {
                    base = VALEUR_SAUT;
                } else if (c.getType() == CaseJeu.Type.TROIS_PAS) {
                    base = VALEUR_TROIS_PAS;
                } else {
                    continue;
                }

                int d = dist[idx];
                if (d >= Distances.INFINI) {
                    continue;
                }

                int dOpp = distAdversaires[idx];
                double contest = estimationContest(d, dOpp);

                double score = base * contest - PENALITE_DISTANCE * d;
                if (score > meilleur) {
                    meilleur = score;
                }
            }
        }

        return meilleur == -1e18 ? 0.0 : meilleur;
    }

    private static double estimationContest(int dMoi, int dOpp) {
        if (dOpp >= Distances.INFINI) {
            return 1.0;
        }
        if (dMoi < dOpp) {
            return 1.0;
        }
        if (dMoi == dOpp) {
            return 0.5;
        }
        if (dMoi <= dOpp + 1) {
            return 0.25;
        }
        return 0.0;
    }

    private static int[] distancesAdversaires(EtatJeu etat, int idJoueur) {
        Labyrinthe laby = etat.getLabyrinthe();
        int n = laby.getLargeur() * laby.getHauteur();
        int[] min = new int[n];
        for (int i = 0; i < n; i++) {
            min[i] = Distances.INFINI;
        }

        for (int i = 0; i < etat.getNbJoueurs(); i++) {
            if (i == idJoueur) {
                continue;
            }
            Joueur j = etat.getJoueur(i);
            int[] dist = Distances.bfs(laby, j.getX(), j.getY());
            for (int k = 0; k < n; k++) {
                if (dist[k] < min[k]) {
                    min[k] = dist[k];
                }
            }
        }

        return min;
    }
}
