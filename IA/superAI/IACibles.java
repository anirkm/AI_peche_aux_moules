package superAI;

import java.util.Arrays;

/**
 * Sélection des cibles et gestion de la concurrence.
 */
class IACibles {
    static int[] listerCasesInteret(Labyrinthe laby, ParametresIA parametres) {
        // Liste compacte des cases utiles (moules + bonus) pour éviter de balayer toute la grille
        int n = laby.getLargeur() * laby.getHauteur();
        int[] tmp = new int[n];
        int count = 0;
        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                if (valeurCase(laby.getCase(x, y), parametres, false) >= 0.0) {
                    tmp[count++] = laby.index(x, y);
                }
            }
        }
        return Arrays.copyOf(tmp, count);
    }
    // --- Adversaires / concurrence ---
    static int[] distancesAdversaires(EtatJeu etat, int idJoueur) {
        // Pour chaque case, on garde la distance minimale d'un adversaire
        // On fait un BFS par adversaire puis on garde la meilleure (min).
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

    static int[] ciblesProbablesAdversaires(EtatJeu etat, int idJoueur,
                                            ParametresIA parametres, boolean finDePartie) {
        // Pour chaque adversaire : on estime sa meilleure cible
        // Résultat : compteur "combien d'adversaires visent cette case".
        Labyrinthe laby = etat.getLabyrinthe();
        int n = laby.getLargeur() * laby.getHauteur();
        int[] counts = new int[n];

        for (int i = 0; i < etat.getNbJoueurs(); i++) {
            if (i == idJoueur) {
                continue;
            }
            Joueur adv = etat.getJoueur(i);
            int[] dist = Distances.bfs(laby, adv.getX(), adv.getY());
            int best = -1;
            double bestScore = -1e18;

            for (int y = 0; y < laby.getHauteur(); y++) {
                for (int x = 0; x < laby.getLargeur(); x++) {
                    int idx = laby.index(x, y);
                    double base = valeurCase(laby.getCase(x, y), parametres, finDePartie);
                    if (base < 0.0) {
                        continue;
                    }
                    int d = dist[idx];
                    if (d >= Distances.INFINI) {
                        continue;
                    }
                    double score = base - parametres.penaliteDistance * d;
                    if (score > bestScore) {
                        bestScore = score;
                        best = idx;
                    }
                }
            }

            if (best != -1) {
                counts[best]++;
            }
        }

        return counts;
    }

    static double appliquerPenaliteAdversaires(double score, Labyrinthe laby, ResultatSimulation sim,
                                               int[] distMoi, int[] distAdversaires,
                                               int[] ciblesAdversaires, ParametresIA parametres) {
        // Si on vise une case "probable" d'un adversaire et qu'il est plus proche,
        // on pénalise pour éviter la course perdue.
        if (parametres.penaliteCibleAdverse <= 0.0 || ciblesAdversaires == null) {
            return score;
        }
        int idx = laby.index(sim.x, sim.y);
        int dMoi = distMoi[idx];
        int dOpp = distAdversaires[idx];
        return score - penaliteCibleAdverse(dMoi, dOpp, ciblesAdversaires, idx, parametres);
    }

    static double estimationContest(int dMoi, int dOpp) {
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

    static double valeurCase(CaseJeu c, ParametresIA parametres, boolean finDePartie) {
        // En fin de partie on ignore les bonus
        // On convertit la case en "valeur" comparable avec les moules.
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

    static double ratioMurs(Labyrinthe laby) {
        // Ratio global mur / total, utile pour détecter les cartes "denses".
        int total = laby.getLargeur() * laby.getHauteur();
        if (total == 0) {
            return 0.0;
        }
        int murs = 0;
        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                if (laby.estMur(x, y)) {
                    murs++;
                }
            }
        }
        return (double) murs / (double) total;
    }

    static int compterCases(Labyrinthe laby, CaseJeu.Type type) {
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

    // --- Choix de cible ---
    static int choisirCible(EtatJeu etat, Joueur joueur, Inventaire inventaire,
                            int[] distAdversaires, int[] ciblesAdversaires,
                            ParametresIA parametres,
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
                score -= penaliteCibleAdverse(d, dOpp, ciblesAdversaires, idx, parametres);

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
                    // Idée : éviter de se bloquer sur une très grosse cible trop loin.
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
                    // Utilisé en fin de partie ou si on stagne.
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

    static int choisirCiblePlanifiee(EtatJeu etat, Joueur joueur, Inventaire inventaire,
                                     int[] distAdversaires, int[] ciblesAdversaires,
                                     ParametresIA parametres,
                                     boolean acceleration, boolean finDePartie) {
        // Plan local sur un top-K de cibles
        // On choisit une cible qui "ouvre" un bon chemin sur plusieurs coups.
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
                score -= penaliteCibleAdverse(d, distAdversaires[idx], ciblesAdversaires, idx, parametres);
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
            score -= penaliteCibleAdverse(d, distAdversaires[idx], ciblesAdversaires, idx, parametres);
            used[i] = true;
            double suite = 0.0;
            if (depth > 1) {
                suite = meilleurSuitePlan(i, depth - 1, used, selIdx, selBase,
                    distCand, distAdversaires, ciblesAdversaires, parametres, parametres.coeffProfondeur);
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

    static DecisionIA decisionVersCible(EtatJeu etat, Joueur joueur, Inventaire inventaire, int cible,
                                        int[] distAdversaires, ParametresIA parametres,
                                        char dernierMouvement, MemoirePositions memoire,
                                        boolean acceleration, int[] casesInteret,
                                        int[][] bfsCache) {
        // Choisit l'action qui réduit le plus la distance vers la cible
        // Et applique un scoring pour trier les actions équivalentes.
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
        for (Action action : IAActions.actionsPossibles(laby, joueur, inventaire)) {
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

        for (Action action : IAActions.actionsPossibles(laby, joueur, inventaire)) {
            ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
            Inventaire invApres = inventaire.copie();
            invApres.appliquer(sim);
            int d = Distances.distanceAvecBonus(laby, sim.x, sim.y,
                invApres.getBonusSaut(), invApres.getBonusTroisPas(), cible);
            if (d != meilleurD) {
                continue;
            }
            int[] dist = Distances.bfsCached(laby, sim.x, sim.y, bfsCache);
            double score = IAActions.scoreAction(laby, sim, null, dist, distAdversaires, null,
                casesInteret, action, memoire, dernierMouvement, inventaire, joueur.getX(), joueur.getY(),
                cible, parametres, acceleration);
            IAActions.enregistrerTop(top, action, score, sim);
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

    private static double scoreCibleAvecDistance(double base, int dist, int distAdversaire,
                                                 ParametresIA parametres) {
        // Score simple basé sur valeur et distance
        // La concurrence réduit la valeur si un adversaire est plus proche.
        double contest = estimationContest(dist, distAdversaire);
        return base * contest - parametres.penaliteDistance * dist;
    }

    private static double meilleurSuitePlan(int prev, int depthLeft, boolean[] used, int[] candIdx,
                                            double[] candBase, int[][] distCand, int[] distAdversaires,
                                            int[] ciblesAdversaires, ParametresIA parametres,
                                            double facteur) {
        // Recherche récursive sur une petite profondeur
        // facteur = discount pour ne pas surévaluer le long terme.
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
            score -= penaliteCibleAdverse(d, distAdversaires[idx], ciblesAdversaires, idx, parametres);
            used[i] = true;
            double suite = meilleurSuitePlan(i, depthLeft - 1, used, candIdx, candBase,
                distCand, distAdversaires, ciblesAdversaires, parametres,
                facteur * parametres.coeffProfondeur);
            used[i] = false;
            // facteur = discount simple pour pas trop sur-évaluer la suite
            double total = facteur * score + suite;
            if (total > best) {
                best = total;
            }
        }
        return best == -1e18 ? 0.0 : best;
    }

    private static double penaliteCibleAdverse(int dMoi, int dOpp, int[] ciblesAdversaires, int idx,
                                               ParametresIA parametres) {
        if (parametres.penaliteCibleAdverse <= 0.0 || ciblesAdversaires == null) {
            return 0.0;
        }
        int nbCibles = ciblesAdversaires[idx];
        if (nbCibles <= 0) {
            return 0.0;
        }
        if (dOpp < dMoi) {
            return parametres.penaliteCibleAdverse * nbCibles;
        }
        if (dOpp == dMoi) {
            return parametres.penaliteCibleAdverse * 0.5 * nbCibles;
        }
        return 0.0;
    }
}
