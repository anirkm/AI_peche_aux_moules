package superAI;

class IA {
    static DecisionIA choisirAction(EtatJeu etat, int idJoueur, Inventaire inventaire, ParametresIA parametres) {
        Labyrinthe laby = etat.getLabyrinthe();
        Joueur joueur = etat.getJoueur(idJoueur);

        // Distance minimale des adversaires vers chaque case
        int[] distAdversaires = distancesAdversaires(etat, idJoueur);

        Action meilleure = Action.simple('C');
        ResultatSimulation meilleurSim = null;
        double meilleurScore = -1e18;
        Action meilleureMouv = null;
        ResultatSimulation meilleurMouvSim = null;
        double meilleurMouvScore = -1e18;

        EvaluationAction[] top = new EvaluationAction[3];

        for (Action action : actionsPossibles(laby, joueur, inventaire)) {
            ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
            int[] dist = Distances.bfs(laby, sim.x, sim.y);

            double score = scoreSimulation(laby, dist, distAdversaires, sim, null, parametres);

            // Évite les coups qui ne font rien
            if (!sim.aBouge() && sim.points == 0 && sim.bonusSautGagne == 0 && sim.bonusTroisPasGagne == 0) {
                score -= parametres.penaliteImmobile;
                if (action.getType() == Action.Type.SIMPLE && action.getD1() == 'C') {
                    score -= parametres.penaliteImmobile;
                }
            }

            // Petit coup d'avance
            Inventaire invApres = inventaire.copie();
            invApres.appliquer(sim);
            double suite = meilleurApres(etat, idJoueur, sim, invApres, distAdversaires, parametres);
            score += parametres.coeffProfondeur * suite;

            enregistrerTop(top, action, score, sim);

            if (score > meilleurScore || (Math.abs(score - meilleurScore) < 1e-9
                && sim.aBouge() && (meilleurSim == null || !meilleurSim.aBouge()))) {
                meilleurScore = score;
                meilleure = action;
                meilleurSim = sim;
            }

            if (sim.aBouge()) {
                if (score > meilleurMouvScore) {
                    meilleurMouvScore = score;
                    meilleureMouv = action;
                    meilleurMouvSim = sim;
                }
            }
        }

        if (meilleurSim != null && !meilleurSim.aBouge() && meilleureMouv != null) {
            return new DecisionIA(meilleureMouv, meilleurMouvSim, top);
        }

        return new DecisionIA(meilleure, meilleurSim, top);
    }

    private static Action[] actionsPossibles(Labyrinthe laby, Joueur joueur, Inventaire inventaire) {
        int base = 5;
        int saut = inventaire.getBonusSaut() > 0 ? 4 : 0;
        int trois = inventaire.getBonusTroisPas() > 0 ? 64 : 0;
        Action[] actions = new Action[base + saut + trois];
        int idx = 0;

        char[] simples = new char[]{'N', 'S', 'E', 'O'};
        for (char dir : simples) {
            if (deplacementValide(laby, joueur.getX(), joueur.getY(), dir)) {
                actions[idx++] = Action.simple(dir);
            }
        }
        actions[idx++] = Action.simple('C');

        if (saut > 0) {
            for (char dir : simples) {
                if (sautValide(laby, joueur.getX(), joueur.getY(), dir)) {
                    actions[idx++] = Action.saut(dir);
                }
            }
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

        if (idx == 0) {
            return new Action[]{Action.simple('C')};
        }
        if (idx == actions.length) {
            return actions;
        }
        Action[] coupe = new Action[idx];
        System.arraycopy(actions, 0, coupe, 0, idx);
        return coupe;
    }

    private static void enregistrerTop(EvaluationAction[] top, Action action, double score, ResultatSimulation sim) {
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

    private static double meilleurApres(EtatJeu etat, int idJoueur, ResultatSimulation sim1,
                                        Inventaire inventaire, int[] distAdversaires, ParametresIA parametres) {
        Labyrinthe laby = etat.getLabyrinthe();
        Joueur joueur = new Joueur(idJoueur, sim1.x, sim1.y);
        double meilleur = -1e18;

        for (Action action : actionsPossibles(laby, joueur, inventaire)) {
            ResultatSimulation sim2 = MoteurSimulation.appliquer(etat, joueur, inventaire, action, sim1);
            int[] dist = Distances.bfs(laby, sim2.x, sim2.y);
            double score = scoreSimulation(laby, dist, distAdversaires, sim2, sim1, parametres);
            if (score > meilleur) {
                meilleur = score;
            }
        }

        return meilleur == -1e18 ? 0.0 : meilleur;
    }

    private static double scoreSimulation(Labyrinthe laby, int[] dist,
                                          int[] distAdversaires, ResultatSimulation sim,
                                          ResultatSimulation deja, ParametresIA parametres) {
        double futur = evaluerFutur(laby, dist, distAdversaires, sim, deja, parametres);
        return sim.points
            + parametres.valeurSaut * sim.bonusSautGagne
            + parametres.valeurTroisPas * sim.bonusTroisPasGagne
            - parametres.penaliteUtilisationSaut * sim.bonusSautUtilise
            - parametres.penaliteUtilisationTroisPas * sim.bonusTroisPasUtilise
            + futur;
    }

    private static double evaluerFutur(Labyrinthe laby, int[] dist,
                                       int[] distAdversaires, ResultatSimulation sim,
                                       ResultatSimulation deja, ParametresIA parametres) {
        double meilleur = -1e18;

        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                int idx = laby.index(x, y);
                if (estCollecte(sim, deja, idx)) {
                    continue;
                }

                CaseJeu c = laby.getCase(x, y);
                double base;
                if (c.getType() == CaseJeu.Type.MOULE) {
                    base = c.getValeur();
                } else if (c.getType() == CaseJeu.Type.SAUT) {
                    base = parametres.valeurSaut;
                } else if (c.getType() == CaseJeu.Type.TROIS_PAS) {
                    base = parametres.valeurTroisPas;
                } else {
                    continue;
                }

                int d = dist[idx];
                if (d >= Distances.INFINI) {
                    continue;
                }

                int dOpp = distAdversaires[idx];
                double contest = estimationContest(d, dOpp);

                double score = base * contest - parametres.penaliteDistance * d;
                if (score > meilleur) {
                    meilleur = score;
                }
            }
        }

        return meilleur == -1e18 ? 0.0 : meilleur;
    }

    private static boolean estCollecte(ResultatSimulation sim, ResultatSimulation deja, int idx) {
        if (sim != null && sim.aCollecte(idx)) {
            return true;
        }
        return deja != null && deja.aCollecte(idx);
    }

    private static boolean deplacementValide(Labyrinthe laby, int x, int y, char dir) {
        int[] d = delta(dir);
        int nx = x + d[0];
        int ny = y + d[1];
        return laby.dansBornes(nx, ny) && !laby.estMur(nx, ny);
    }

    private static boolean sautValide(Labyrinthe laby, int x, int y, char dir) {
        int[] d = delta(dir);
        int nx = x + d[0] * 2;
        int ny = y + d[1] * 2;
        if (laby.dansBornes(nx, ny) && !laby.estMur(nx, ny)) {
            return true;
        }
        return deplacementValide(laby, x, y, dir);
    }

    private static int[] delta(char dir) {
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
