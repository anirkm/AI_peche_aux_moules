package superAI;

/**
 * Cœur de la décision.
 * Combine distances, scoring, mémoire et planification locale
 * pour choisir l'action envoyée au serveur à chaque tour.
 */
class IA {
    /*
     * Stratégie globale :
     * - calcule des distances (avec et sans bonus),
     * - évalue chaque action (points immédiats + futur proche),
     * - choisit une cible puis l’action qui s’en rapproche le plus,
     * - applique des pénalités pour éviter les boucles.
     *
     * Notes :
     * - Les distances sont exactes (BFS) car chaque action coûte 1 tour.
     * - Le score est un calcul simple : il ne prédit pas tout, mais guide la décision.
     * - Le mode "plan" simule une courte séquence de cibles (top‑K) pour limiter les zigzags.
     * - La mémoire évite les oscillations (aller-retour) et stabilise la trajectoire.
     *
     * Objectif : maximiser les points tout en minimisant le nombre de tours.
     */
    static DecisionIA choisirAction(EtatJeu etat, int idJoueur, Inventaire inventaire,
                                    ParametresIA parametres, char dernierMouvement,
                                    MemoirePositions memoire, MemoireCible memoireCible,
                                    int toursSansPoints) {
        // Raccourcis utiles sur l'état courant
        Labyrinthe laby = etat.getLabyrinthe();
        Joueur joueur = etat.getJoueur(idJoueur);

        // Distance minimale des adversaires vers chaque case (pour la concurrence).
        // Si un adversaire est plus proche, la case vaut moins (risque de perdre la course).
        int[] distAdversaires = distancesAdversaires(etat, idJoueur);
        // Compte des moules restantes pour ajuster le mode
        int nbMoules = compterCases(laby, CaseJeu.Type.MOULE);
        // Mode accel: on force la distance quand on stagne ou qu'il reste peu de moules.
        boolean acceleration = parametres.modeHybride != 0
            && (toursSansPoints >= parametres.seuilAcceleration || nbMoules <= parametres.seuilRarete);
        // Fin de partie: on vise uniquement les moules
        boolean finDePartie = parametres.modeHybride != 0 && nbMoules <= parametres.seuilRarete;

        // Valeurs par défaut si rien de mieux
        Action meilleure = Action.simple('C');
        ResultatSimulation meilleurSim = null;
        double meilleurScore = -1e18;
        Action meilleureMouv = null;
        ResultatSimulation meilleurMouvSim = null;
        double meilleurMouvScore = -1e18;

        // On garde aussi un top 3 pour le logging
        EvaluationAction[] top = new EvaluationAction[3];

        // Petit plan local (suite courte de cibles) pour éviter les zigzags.
        // Idée : on teste plusieurs cibles "prometteuses" puis on choisit la meilleure séquence.
        if (parametres.modePlan != 0) {
            int ciblePlan = choisirCiblePlanifiee(etat, joueur, inventaire, distAdversaires,
                parametres, acceleration, finDePartie);
            // Si un plan donne déjà une décision cohérente, on sort vite
            if (ciblePlan != -1) {
                DecisionIA decision = decisionVersCible(etat, joueur, inventaire, ciblePlan, distAdversaires,
                    parametres, dernierMouvement, memoire, acceleration);
                if (decision != null) {
                    return decision;
                }
            }
        }

        // Reset cible si on stagne trop ou si on accélère fortement.
        // Cela force un changement de direction quand on est "bloqué".
        if (memoireCible != null && toursSansPoints >= parametres.toursSansPointsMax) {
            memoireCible.reset();
        }
        if (memoireCible != null && acceleration) {
            memoireCible.reset();
        }

        // Cible verrouillee si possible (évite les changements trop fréquents).
        // On compare le score actuel de la cible avec la meilleure alternative.
        int cible = choisirCible(etat, joueur, inventaire, distAdversaires,
            parametres, memoireCible, acceleration, finDePartie);
        if (cible != -1) {
            // Action directe vers la cible
            DecisionIA decision = decisionVersCible(etat, joueur, inventaire, cible, distAdversaires,
                parametres, dernierMouvement, memoire, acceleration);
            if (decision != null) {
                return decision;
            }
        }

        // Évalue toutes les actions possibles, puis garde la meilleure.
        // L'évaluation utilise :
        //  - scoreSimulation() (points + bonus + futur),
        //  - appliquerPenalites() (anti‑boucle, demi‑tour, immobile),
        //  - un lookahead court (meilleurApres).
        for (Action action : actionsPossibles(laby, joueur, inventaire)) {
            // Simule l'effet immédiat
            ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
            // Distances depuis la case obtenue
            int[] dist = Distances.bfs(laby, sim.x, sim.y);

            double score = scoreSimulation(laby, dist, distAdversaires, sim, null, parametres);
            score = appliquerPenalites(score, action, sim, laby, memoire, dernierMouvement, parametres);

            // Petit coup d'avance : regarde la meilleure action suivante (profondeur 2)
            Inventaire invApres = inventaire.copie();
            // Applique les gains/consos de bonus
            invApres.appliquer(sim);
            double suite = meilleurApres(etat, idJoueur, sim, invApres, distAdversaires, parametres);
            score += parametres.coeffProfondeur * suite;

            // Garde une trace des meilleurs coups
            enregistrerTop(top, action, score, sim);

            // Meilleure action globale
            if (score > meilleurScore || (Math.abs(score - meilleurScore) < 1e-9
                && sim.aBouge() && (meilleurSim == null || !meilleurSim.aBouge()))) {
                meilleurScore = score;
                meilleure = action;
                meilleurSim = sim;
            }

            // Meilleure action qui bouge
            if (sim.aBouge()) {
                if (score > meilleurMouvScore) {
                    meilleurMouvScore = score;
                    meilleureMouv = action;
                    meilleurMouvSim = sim;
                }
            }
        }

        // Si la meilleure action ne bouge pas, on préfère une action qui bouge.
        // Cela évite de "passer" alors qu'un déplacement est possible.
        if (meilleurSim != null && !meilleurSim.aBouge() && meilleureMouv != null) {
            return new DecisionIA(meilleureMouv, meilleurMouvSim, top);
        }

        return new DecisionIA(meilleure, meilleurSim, top);
    }

    private static Action[] actionsPossibles(Labyrinthe laby, Joueur joueur, Inventaire inventaire) {
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
            return new Action[]{Action.simple('C')};
        }
        // Coupe du tableau pour ne garder que les actions réelles
        Action[] coupe = new Action[idx];
        System.arraycopy(actions, 0, coupe, 0, idx);
        return coupe;
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

    private static void enregistrerTop(EvaluationAction[] top, Action action, double score, ResultatSimulation sim) {
        // Insère dans un top-3 simple
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
        // Évalue un coup d'avance (profondeur 2)
        Labyrinthe laby = etat.getLabyrinthe();
        // Joueur "virtuel" sur la nouvelle position
        Joueur joueur = new Joueur(idJoueur, sim1.x, sim1.y);
        double meilleur = -1e18;

        for (Action action : actionsPossibles(laby, joueur, inventaire)) {
            // On simule un 2e coup
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
        // Score instantané + estimation du futur proche
        // Formule globale : points immédiats + valeur des bonus - coût d'usage + futur
        double futur = evaluerFutur(laby, dist, distAdversaires, sim, deja, parametres);
        // Ajout des points directs et des bonus
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
        // Cherche la meilleure case "rentable" à moyen terme
        // On parcourt toutes les cases et on garde le meilleur score
        double meilleur = -1e18;

        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                int idx = laby.index(x, y);
                // Ignore ce qui a déjà été ramassé
                if (estCollecte(sim, deja, idx)) {
                    continue;
                }

                CaseJeu c = laby.getCase(x, y);
                double base;
                // Valeur brute de la case
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
                // Poids selon la concurrence
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
        // Évite de re-compter une case déjà prise dans la simulation
        if (sim != null && sim.aCollecte(idx)) {
            return true;
        }
        return deja != null && deja.aCollecte(idx);
    }

    private static boolean deplacementValide(Labyrinthe laby, int x, int y, char dir) {
        // Mouvement simple
        int[] d = delta(dir);
        int nx = x + d[0];
        int ny = y + d[1];
        return laby.dansBornes(nx, ny) && !laby.estMur(nx, ny);
    }

    private static boolean sautValide(Labyrinthe laby, int x, int y, char dir) {
        // Saut : 2 cases d'un coup
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

    private static double estimationContest(int dMoi, int dOpp) {
        // Pénalise les cases que les adversaires atteignent plus vite
        // 1.0 si je suis le plus proche, 0.5 si égalité, puis chute rapide
        // Si aucun adversaire ne peut y aller, on garde la valeur pleine
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
        // Pour chaque case, on garde la distance minimale d'un adversaire
        Labyrinthe laby = etat.getLabyrinthe();
        int n = laby.getLargeur() * laby.getHauteur();
        int[] min = new int[n];
        // Init à infini
        for (int i = 0; i < n; i++) {
            min[i] = Distances.INFINI;
        }

        for (int i = 0; i < etat.getNbJoueurs(); i++) {
            // Ignore le joueur courant
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
        // Cible principale (moule ou bonus) selon score et distance
        Labyrinthe laby = etat.getLabyrinthe();
        // Distances "réelles" avec bonus en stock
        int[] distMoi = Distances.bfsAvecBonus(laby, joueur.getX(), joueur.getY(),
            inventaire.getBonusSaut(), inventaire.getBonusTroisPas());

        int meilleureCase = -1;
        double meilleurScore = -1e18;
        double meilleurPoints = -1e18;
        int meilleureDistance = Distances.INFINI;
        double scoreActuel = -1e18;

        // Cible en mémoire (si elle existe)
        int cibleActuelle = memoire != null ? memoire.getCible() : -1;

        // Parcours de toutes les cases pour calculer le meilleur score de cible
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

                // Score de la cible déjà verrouillée
                if (idx == cibleActuelle) {
                    scoreActuel = score;
                }
                // Meilleure valeur brute rencontrée
                if (pointsScore > meilleurPoints) {
                    meilleurPoints = pointsScore;
                }

                if (parametres.modeCompromis != 0) {
                    // Compromis : on garde les cibles presque aussi bonnes mais plus proches
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
                    // Mode accélération : on privilégie la distance minimale
                    if (d < meilleureDistance || (d == meilleureDistance && score > meilleurScore)) {
                        meilleureDistance = d;
                        meilleurScore = score;
                        meilleureCase = idx;
                    }
                } else {
                    // Mode normal : on maximise le score
                    if (score > meilleurScore) {
                        meilleurScore = score;
                        meilleureCase = idx;
                    }
                }
            }
        }

        if (memoire == null) {
            // Pas de mémoire de cible
            return meilleureCase;
        }

        // Mise à jour de la mémoire de cible (verrouillage)
        memoire.mettreAJour(laby, meilleureCase, meilleurScore, scoreActuel, parametres);
        return memoire.getCible();
    }

    // Planification locale : on regarde un petit pool de cibles et on choisit
    // la première cible d'une séquence courte qui maximise le score global.
    private static int choisirCiblePlanifiee(EtatJeu etat, Joueur joueur, Inventaire inventaire,
                                             int[] distAdversaires, ParametresIA parametres,
                                             boolean acceleration, boolean finDePartie) {
        // Plan local sur un top-K de cibles
        Labyrinthe laby = etat.getLabyrinthe();
        // Distances depuis la position du joueur
        int[] distMoi = Distances.bfsAvecBonus(laby, joueur.getX(), joueur.getY(),
            inventaire.getBonusSaut(), inventaire.getBonusTroisPas());

        int n = laby.getLargeur() * laby.getHauteur();
        // Tableaux temporaires
        int[] idxCibles = new int[n];
        double[] baseCibles = new double[n];
        double[] scoreCibles = new double[n];
        int[] distCibles = new int[n];
        int nbCibles = 0;

        // 1) Construire la liste de toutes les cibles potentielles
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

                // Score par distance + concurrence
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

        // 2) Garder seulement le top-K (tri partiel)
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

        // Copie compacte des K meilleurs
        int[] selIdx = new int[k];
        double[] selBase = new double[k];
        int[] selDist = new int[k];
        for (int i = 0; i < k; i++) {
            selIdx[i] = idxCibles[i];
            selBase[i] = baseCibles[i];
            selDist[i] = distCibles[i];
        }

        // 3) Pré-calculer les distances entre cibles (pour simuler les séquences)
        int depth = Math.min(Math.max(1, parametres.profondeurPlan), k);
        int[][] distCand = new int[k][];
        for (int i = 0; i < k; i++) {
            int cx = selIdx[i] % laby.getLargeur();
            int cy = selIdx[i] / laby.getLargeur();
            // BFS depuis la cible i
            distCand[i] = Distances.bfs(laby, cx, cy);
        }

        // 4) Tester toutes les séquences de longueur <= profondeurPlan
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
        // Recherche récursive sur une petite profondeur
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
            // facteur = discount simple pour pas trop sur-évaluer la suite
            double total = facteur * score + suite;
            if (total > best) {
                best = total;
            }
        }
        return best == -1e18 ? 0.0 : best;
    }

    private static double scoreCibleAvecDistance(double base, int dist, int distAdversaire,
                                                 ParametresIA parametres) {
        // Score simple basé sur valeur et distance
        double contest = estimationContest(dist, distAdversaire);
        return base * contest - parametres.penaliteDistance * dist;
    }

    private static double valeurCase(CaseJeu c, ParametresIA parametres, boolean finDePartie) {
        // En fin de partie on ignore les bonus
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
        // Choisit l'action qui réduit le plus la distance vers la cible
        Labyrinthe laby = etat.getLabyrinthe();
        // Distance en tenant compte des bonus dispo.
        int d0 = Distances.distanceAvecBonus(laby, joueur.getX(), joueur.getY(),
            inventaire.getBonusSaut(), inventaire.getBonusTroisPas(), cible);
        if (d0 >= Distances.INFINI) {
            // Cible non atteignable
            return null;
        }

        // On cherche la meilleure distance atteignable en 1 action
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
            // Aucun coup ne rapproche
            return null;
        }

        // Parmi les actions qui atteignent cette distance minimale, on choisit la meilleure
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

            // Gain réel en tours pour atteindre la cible
            int gain = d0 - d;
            // Bonus utile si gain réel significatif
            if (gain >= gainMin && action.getType() != Action.Type.SIMPLE) {
                if (score > meilleurBonusScore) {
                    meilleurBonusScore = score;
                    meilleurBonus = action;
                    meilleurBonusSim = sim;
                }
            }
        }

        // Si un bonus donne un vrai gain (>= seuil), on le préfère
        if (meilleurBonus != null) {
            return new DecisionIA(meilleurBonus, meilleurBonusSim, top);
        }

        if (meilleure == null) {
            // Sécurité si tout est invalide
            return null;
        }
        return new DecisionIA(meilleure, meilleurSim, top);
    }

    private static double appliquerPenalites(double score, Action action, ResultatSimulation sim,
                                             Labyrinthe laby, MemoirePositions memoire,
                                             char dernierMouvement, ParametresIA parametres) {
        // Pénalités anti-boucle, demi-tour et immobilité
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
            // Rien de gagné et aucune case franchie
            score -= parametres.penaliteImmobile;
            if (action.getType() == Action.Type.SIMPLE && action.getD1() == 'C') {
                score -= parametres.penaliteImmobile;
            }
        }
        return score;
    }

    private static int compterCases(Labyrinthe laby, CaseJeu.Type type) {
        // Compte les cases d'un type donné
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
