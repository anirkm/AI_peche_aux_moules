package superAI;

class IA {
    static DecisionIA choisirAction(EtatJeu etat, int idJoueur, Inventaire inventaire,
                                    ParametresIA parametres, char dernierMouvement,
                                    MemoirePositions memoire, MemoireCible memoireCible,
                                    int toursSansPoints) {
        Labyrinthe laby = etat.getLabyrinthe();
        Joueur joueur = etat.getJoueur(idJoueur);

        // Distance minimale des adversaires vers chaque case
        int[] distAdversaires = distancesAdversaires(etat, idJoueur);
        int nbMoules = compterCases(laby, CaseJeu.Type.MOULE);
        // Mode accel: on force la distance quand on stagne ou qu'il reste peu de moules.
        boolean acceleration = parametres.modeHybride != 0
            && (toursSansPoints >= parametres.seuilAcceleration || nbMoules <= parametres.seuilRarete);
        boolean finDePartie = parametres.modeHybride != 0 && nbMoules <= parametres.seuilRarete;

        Action meilleure = Action.simple('C');
        ResultatSimulation meilleurSim = null;
        double meilleurScore = -1e18;
        Action meilleureMouv = null;
        ResultatSimulation meilleurMouvSim = null;
        double meilleurMouvScore = -1e18;

        EvaluationAction[] top = new EvaluationAction[3];

        // Petit plan local (suite courte de cibles)
        if (parametres.modePlan != 0) {
            int ciblePlan = choisirCiblePlanifiee(etat, joueur, inventaire, distAdversaires,
                parametres, acceleration, finDePartie);
            if (ciblePlan != -1) {
                DecisionIA decision = decisionVersCible(etat, joueur, inventaire, ciblePlan, distAdversaires,
                    parametres, dernierMouvement, memoire, acceleration);
                if (decision != null) {
                    return decision;
                }
            }
        }

        // Reset cible si on stagne trop
        if (memoireCible != null && toursSansPoints >= parametres.toursSansPointsMax) {
            memoireCible.reset();
        }
        if (memoireCible != null && acceleration) {
            memoireCible.reset();
        }

        // Cible verrouillee si possible
        int cible = choisirCible(etat, joueur, inventaire, distAdversaires,
            parametres, memoireCible, acceleration, finDePartie);
        if (cible != -1) {
            DecisionIA decision = decisionVersCible(etat, joueur, inventaire, cible, distAdversaires,
                parametres, dernierMouvement, memoire, acceleration);
            if (decision != null) {
                return decision;
            }
        }

        for (Action action : actionsPossibles(laby, joueur, inventaire)) {
            ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
            int[] dist = Distances.bfs(laby, sim.x, sim.y);

            double score = scoreSimulation(laby, dist, distAdversaires, sim, null, parametres);
            score = appliquerPenalites(score, action, sim, laby, memoire, dernierMouvement, parametres);

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
        int max = 1 + 4 + 4 + 64;
        Action[] actions = new Action[max];
        int idx = 0;
        int saut = inventaire.getBonusSaut() > 0 ? 4 : 0;
        int trois = inventaire.getBonusTroisPas() > 0 ? 64 : 0;

        char[] simples = new char[]{'N', 'S', 'E', 'O'};
        for (char dir : simples) {
            if (deplacementValide(laby, joueur.getX(), joueur.getY(), dir)) {
                actions[idx++] = Action.simple(dir);
            }
        }

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
        Action[] coupe = new Action[idx];
        System.arraycopy(actions, 0, coupe, 0, idx);
        return coupe;
    }

    private static boolean estRetour(Action action, char dernierMouvement) {
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
        return laby.dansBornes(nx, ny) && !laby.estMur(nx, ny);
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

    private static int choisirCible(EtatJeu etat, Joueur joueur, Inventaire inventaire,
                                    int[] distAdversaires, ParametresIA parametres,
                                    MemoireCible memoire, boolean acceleration,
                                    boolean finDePartie) {
        Labyrinthe laby = etat.getLabyrinthe();
        int[] distMoi = Distances.bfsAvecBonus(laby, joueur.getX(), joueur.getY(),
            inventaire.getBonusSaut(), inventaire.getBonusTroisPas());

        int meilleureCase = -1;
        double meilleurScore = -1e18;
        double meilleurPoints = -1e18;
        int meilleureDistance = Distances.INFINI;
        double scoreActuel = -1e18;

        int cibleActuelle = memoire != null ? memoire.getCible() : -1;

        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                int idx = laby.index(x, y);
                double base = valeurCase(laby.getCase(x, y), parametres, finDePartie);
                if (base < 0.0) {
                    continue;
                }

                int d = distMoi[idx];
                if (d >= Distances.INFINI) {
                    continue;
                }

                int dOpp = distAdversaires[idx];
                double contest = estimationContest(d, dOpp);
                double pointsScore = base * contest;
                double score = pointsScore - parametres.penaliteDistance * d;

                if (idx == cibleActuelle) {
                    scoreActuel = score;
                }
                if (pointsScore > meilleurPoints) {
                    meilleurPoints = pointsScore;
                }

                if (parametres.modeCompromis != 0) {
                    double marge = finDePartie ? parametres.margeCompromisFin : parametres.margeCompromis;
                    double seuilPoints = meilleurPoints * (1.0 - marge);
                    if (pointsScore + 1e-9 >= seuilPoints) {
                        if (d < meilleureDistance || (d == meilleureDistance && score > meilleurScore)) {
                            meilleureDistance = d;
                            meilleurScore = score;
                            meilleureCase = idx;
                        }
                    }
                } else if (acceleration) {
                    if (d < meilleureDistance || (d == meilleureDistance && score > meilleurScore)) {
                        meilleureDistance = d;
                        meilleurScore = score;
                        meilleureCase = idx;
                    }
                } else {
                    if (score > meilleurScore) {
                        meilleurScore = score;
                        meilleureCase = idx;
                    }
                }
            }
        }

        if (memoire == null) {
            return meilleureCase;
        }

        memoire.mettreAJour(laby, meilleureCase, meilleurScore, scoreActuel, parametres);
        return memoire.getCible();
    }

    // Planification locale : on regarde un petit pool de cibles et on choisit
    // la première cible d'une séquence courte qui maximise le score global.
    private static int choisirCiblePlanifiee(EtatJeu etat, Joueur joueur, Inventaire inventaire,
                                             int[] distAdversaires, ParametresIA parametres,
                                             boolean acceleration, boolean finDePartie) {
        Labyrinthe laby = etat.getLabyrinthe();
        int[] distMoi = Distances.bfsAvecBonus(laby, joueur.getX(), joueur.getY(),
            inventaire.getBonusSaut(), inventaire.getBonusTroisPas());

        int n = laby.getLargeur() * laby.getHauteur();
        int[] idxCibles = new int[n];
        double[] baseCibles = new double[n];
        double[] scoreCibles = new double[n];
        int[] distCibles = new int[n];
        int nbCibles = 0;

        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                int idx = laby.index(x, y);
                double base = valeurCase(laby.getCase(x, y), parametres, finDePartie);
                if (base < 0.0) {
                    continue;
                }

                int d = distMoi[idx];
                if (d >= Distances.INFINI) {
                    continue;
                }

                double score = scoreCibleAvecDistance(base, d, distAdversaires[idx], parametres);
                idxCibles[nbCibles] = idx;
                baseCibles[nbCibles] = base;
                scoreCibles[nbCibles] = score;
                distCibles[nbCibles] = d;
                nbCibles++;
            }
        }

        if (nbCibles == 0) {
            return -1;
        }

        int k = Math.min(Math.max(1, parametres.nbCiblesPlan), nbCibles);
        // On garde juste un petit top-K, tri maison.
        for (int i = 0; i < k; i++) {
            int best = i;
            for (int j = i + 1; j < nbCibles; j++) {
                if (scoreCibles[j] > scoreCibles[best]) {
                    best = j;
                }
            }
            if (best != i) {
                int tmpIdx = idxCibles[i];
                idxCibles[i] = idxCibles[best];
                idxCibles[best] = tmpIdx;

                double tmpBase = baseCibles[i];
                baseCibles[i] = baseCibles[best];
                baseCibles[best] = tmpBase;

                double tmpScore = scoreCibles[i];
                scoreCibles[i] = scoreCibles[best];
                scoreCibles[best] = tmpScore;

                int tmpDist = distCibles[i];
                distCibles[i] = distCibles[best];
                distCibles[best] = tmpDist;
            }
        }

        int[] selIdx = new int[k];
        double[] selBase = new double[k];
        int[] selDist = new int[k];
        for (int i = 0; i < k; i++) {
            selIdx[i] = idxCibles[i];
            selBase[i] = baseCibles[i];
            selDist[i] = distCibles[i];
        }

        int depth = Math.min(Math.max(1, parametres.profondeurPlan), k);
        int[][] distCand = new int[k][];
        for (int i = 0; i < k; i++) {
            int cx = selIdx[i] % laby.getLargeur();
            int cy = selIdx[i] / laby.getLargeur();
            distCand[i] = Distances.bfs(laby, cx, cy);
        }

        boolean[] used = new boolean[k];
        double bestTotal = -1e18;
        int bestIdx = selIdx[0];
        for (int i = 0; i < k; i++) {
            int idx = selIdx[i];
            int d = selDist[i];
            if (d >= Distances.INFINI) {
                continue;
            }
            double score = scoreCibleAvecDistance(selBase[i], d, distAdversaires[idx], parametres);
            used[i] = true;
            double suite = 0.0;
            if (depth > 1) {
                suite = meilleurSuitePlan(i, depth - 1, used, selIdx, selBase,
                    distCand, distAdversaires, parametres, parametres.coeffProfondeur);
            }
            used[i] = false;
            double total = score + suite;
            if (total > bestTotal) {
                bestTotal = total;
                bestIdx = idx;
            }
        }

        return bestIdx;
    }

    private static double meilleurSuitePlan(int prev, int depthLeft, boolean[] used, int[] candIdx,
                                            double[] candBase, int[][] distCand, int[] distAdversaires,
                                            ParametresIA parametres, double facteur) {
        if (depthLeft <= 0) {
            return 0.0;
        }
        double best = -1e18;
        int n = candIdx.length;
        for (int i = 0; i < n; i++) {
            if (used[i]) {
                continue;
            }
            int idx = candIdx[i];
            int d = distCand[prev][idx];
            if (d >= Distances.INFINI) {
                continue;
            }
            double score = scoreCibleAvecDistance(candBase[i], d, distAdversaires[idx], parametres);
            used[i] = true;
            double suite = meilleurSuitePlan(i, depthLeft - 1, used, candIdx, candBase,
                distCand, distAdversaires, parametres, facteur * parametres.coeffProfondeur);
            used[i] = false;
            // facteur = discount simple pour pas trop sur-evaluer la suite
            double total = facteur * score + suite;
            if (total > best) {
                best = total;
            }
        }
        return best == -1e18 ? 0.0 : best;
    }

    private static double scoreCibleAvecDistance(double base, int dist, int distAdversaire,
                                                 ParametresIA parametres) {
        double contest = estimationContest(dist, distAdversaire);
        return base * contest - parametres.penaliteDistance * dist;
    }

    private static double valeurCase(CaseJeu c, ParametresIA parametres, boolean finDePartie) {
        if (finDePartie && c.getType() != CaseJeu.Type.MOULE) {
            return -1.0;
        }
        if (c.getType() == CaseJeu.Type.MOULE) {
            return c.getValeur();
        }
        if (c.getType() == CaseJeu.Type.SAUT) {
            return parametres.valeurSaut;
        }
        if (c.getType() == CaseJeu.Type.TROIS_PAS) {
            return parametres.valeurTroisPas;
        }
        return -1.0;
    }

    private static DecisionIA decisionVersCible(EtatJeu etat, Joueur joueur, Inventaire inventaire, int cible,
                                                int[] distAdversaires, ParametresIA parametres,
                                                char dernierMouvement, MemoirePositions memoire,
                                                boolean acceleration) {
        Labyrinthe laby = etat.getLabyrinthe();
        // Distance en tenant compte des bonus dispo.
        int d0 = Distances.distanceAvecBonus(laby, joueur.getX(), joueur.getY(),
            inventaire.getBonusSaut(), inventaire.getBonusTroisPas(), cible);
        if (d0 >= Distances.INFINI) {
            return null;
        }

        int meilleurD = Distances.INFINI;
        for (Action action : actionsPossibles(laby, joueur, inventaire)) {
            ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
            Inventaire invApres = inventaire.copie();
            invApres.appliquer(sim);
            int d = Distances.distanceAvecBonus(laby, sim.x, sim.y,
                invApres.getBonusSaut(), invApres.getBonusTroisPas(), cible);
            if (d < meilleurD) {
                meilleurD = d;
            }
        }
        if (meilleurD >= Distances.INFINI) {
            return null;
        }

        EvaluationAction[] top = new EvaluationAction[3];
        Action meilleure = null;
        ResultatSimulation meilleurSim = null;
        double meilleurScore = -1e18;
        Action meilleurBonus = null;
        ResultatSimulation meilleurBonusSim = null;
        double meilleurBonusScore = -1e18;
        int gainMin = acceleration ? parametres.gainDistanceBonusMinAccel : parametres.gainDistanceBonusMin;

        for (Action action : actionsPossibles(laby, joueur, inventaire)) {
            ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
            Inventaire invApres = inventaire.copie();
            invApres.appliquer(sim);
            int d = Distances.distanceAvecBonus(laby, sim.x, sim.y,
                invApres.getBonusSaut(), invApres.getBonusTroisPas(), cible);
            if (d != meilleurD) {
                continue;
            }
            double score = scoreSimulation(laby, Distances.bfs(laby, sim.x, sim.y), distAdversaires, sim, null, parametres);
            score = appliquerPenalites(score, action, sim, laby, memoire, dernierMouvement, parametres);
            enregistrerTop(top, action, score, sim);
            if (score > meilleurScore) {
                meilleurScore = score;
                meilleure = action;
                meilleurSim = sim;
            }

            int gain = d0 - d;
            if (gain >= gainMin && action.getType() != Action.Type.SIMPLE) {
                if (score > meilleurBonusScore) {
                    meilleurBonusScore = score;
                    meilleurBonus = action;
                    meilleurBonusSim = sim;
                }
            }
        }

        if (meilleurBonus != null) {
            return new DecisionIA(meilleurBonus, meilleurBonusSim, top);
        }

        if (meilleure == null) {
            return null;
        }
        return new DecisionIA(meilleure, meilleurSim, top);
    }

    private static double appliquerPenalites(double score, Action action, ResultatSimulation sim,
                                             Labyrinthe laby, MemoirePositions memoire,
                                             char dernierMouvement, ParametresIA parametres) {
        if (estRetour(action, dernierMouvement)) {
            score -= parametres.penaliteRetour;
        }
        if (memoire != null) {
            int idx = laby.index(sim.x, sim.y);
            if (memoire.contient(idx)) {
                score -= parametres.penaliteBoucle;
            }
        }
        if (!sim.aBouge() && sim.points == 0 && sim.bonusSautGagne == 0 && sim.bonusTroisPasGagne == 0) {
            score -= parametres.penaliteImmobile;
            if (action.getType() == Action.Type.SIMPLE && action.getD1() == 'C') {
                score -= parametres.penaliteImmobile;
            }
        }
        return score;
    }

    private static int compterCases(Labyrinthe laby, CaseJeu.Type type) {
        int total = 0;
        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                if (laby.getCase(x, y).getType() == type) {
                    total++;
                }
            }
        }
        return total;
    }
}
