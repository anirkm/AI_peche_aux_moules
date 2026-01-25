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
        int[] distAdversaires = IACibles.distancesAdversaires(etat, idJoueur);
        // Cache BFS intra-tour (évite de recalculer plusieurs fois la même source)
        int[][] bfsCache = new int[laby.getLargeur() * laby.getHauteur()][];
        // Liste des cases utiles (moules + bonus) pour accélérer les scans
        int[] casesInteret = IACibles.listerCasesInteret(laby, parametres);
        // Compte des moules restantes pour ajuster le mode
        int nbMoules = IACibles.compterCases(laby, CaseJeu.Type.MOULE);
        // Mode accel: on force la distance quand on stagne ou qu'il reste peu de moules.
        boolean acceleration = parametres.modeHybride != 0
            && (toursSansPoints >= parametres.seuilAcceleration || nbMoules <= parametres.seuilRarete);
        // Fin de partie: on vise uniquement les moules
        boolean finDePartie = parametres.modeHybride != 0 && nbMoules <= parametres.seuilRarete;
        // Estime les cases que les adversaires vont viser (optionnel)
        int[] ciblesAdversaires = null;
        if (parametres.penaliteCibleAdverse > 0.0) {
            ciblesAdversaires = IACibles.ciblesProbablesAdversaires(etat, idJoueur, parametres, finDePartie);
        }

        // Valeurs par défaut si rien de mieux
        Action meilleure = Action.simple('C');
        ResultatSimulation meilleurSim = null;
        double meilleurScore = -1e18;
        Action meilleureMouv = null;
        ResultatSimulation meilleurMouvSim = null;
        double meilleurMouvScore = -1e18;
        Action meilleureExplore = null;
        ResultatSimulation meilleureExploreSim = null;
        double meilleureExploreScore = -1e18;
        int meilleureExploreFreq = Integer.MAX_VALUE;

        // On garde aussi un top 3 pour le logging
        // (n'influe pas sur la décision, c'est juste pour analyser)
        EvaluationAction[] top = new EvaluationAction[3];

        // Reset cible si on stagne trop ou si on accélère fortement.
        // Cela force un changement de direction quand on est "bloqué".
        if (memoireCible != null && toursSansPoints >= parametres.toursSansPointsMax) {
            memoireCible.reset();
        }
        if (memoireCible != null && acceleration) {
            memoireCible.reset();
        }

        // Cible de référence (utile pour juger l'usage des bonus)
        int cibleReference = -1;
        if (memoireCible != null) {
            cibleReference = memoireCible.getCible();
        }
        if (cibleReference == -1) {
            cibleReference = IACibles.choisirCible(etat, joueur, inventaire, distAdversaires,
                ciblesAdversaires, parametres, null, acceleration, finDePartie);
        }

        // Recherche par faisceau sur des séquences d'actions (plus coûteux, plus stable)
        int curIdx = laby.index(joueur.getX(), joueur.getY());
        boolean boucle = memoire != null && memoire.compter(curIdx) >= 2;
        boolean beamActif;
        if (parametres.modeBeam == 2) {
            double ratioMurs = IACibles.ratioMurs(laby);
            boolean complexe = ratioMurs >= parametres.beamAutoMur
                || nbMoules >= parametres.beamAutoMinMoules
                || (laby.getLargeur() * laby.getHauteur()) >= parametres.beamAutoMinCases;
            beamActif = complexe && !acceleration && !finDePartie;
        } else {
            beamActif = parametres.modeBeam != 0;
        }

        boolean beamOk = beamActif
            && toursSansPoints < parametres.toursSansPointsMax
            && !boucle;
        if (beamOk) {
            // Beam search = coût CPU plus élevé, mais meilleure stabilité
            DecisionIA decision = IABeam.decisionBeam(etat, idJoueur, inventaire, distAdversaires, parametres,
                dernierMouvement, memoire, cibleReference, ciblesAdversaires, acceleration,
                casesInteret, bfsCache);
            if (decision != null) {
                return decision;
            }
        }

        // Petit plan local (suite courte de cibles) pour éviter les zigzags.
        // Idée : on teste plusieurs cibles "prometteuses" puis on choisit la meilleure séquence.
        if (parametres.modePlan != 0) {
            int ciblePlan = IACibles.choisirCiblePlanifiee(etat, joueur, inventaire, distAdversaires, ciblesAdversaires,
                parametres, acceleration, finDePartie);
            // Si un plan donne déjà une décision cohérente, on sort vite
            if (ciblePlan != -1) {
                DecisionIA decision = IACibles.decisionVersCible(etat, joueur, inventaire, ciblePlan, distAdversaires,
                    parametres, dernierMouvement, memoire, acceleration, casesInteret, bfsCache);
                if (decision != null) {
                    return decision;
                }
            }
        }

        // Cible verrouillee si possible (évite les changements trop fréquents).
        // On compare le score actuel de la cible avec la meilleure alternative.
        int cible = IACibles.choisirCible(etat, joueur, inventaire, distAdversaires, ciblesAdversaires,
            parametres, memoireCible, acceleration, finDePartie);
        if (cible != -1) {
            // Action directe vers la cible
            DecisionIA decision = IACibles.decisionVersCible(etat, joueur, inventaire, cible, distAdversaires,
                parametres, dernierMouvement, memoire, acceleration, casesInteret, bfsCache);
            if (decision != null) {
                return decision;
            }
        }

        // Évalue toutes les actions possibles, puis garde la meilleure.
        // L'évaluation utilise :
        //  - scoreSimulation() (points + bonus + futur),
        //  - appliquerPenalites() (anti‑boucle, demi‑tour, immobile),
        //  - un lookahead court (meilleurApres).
        for (Action action : IAActions.actionsPossibles(laby, joueur, inventaire)) {
            // Simule l'effet immédiat
            ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
            // Distances depuis la case obtenue
            int[] dist = Distances.bfsCached(laby, sim.x, sim.y, bfsCache);

            double score = IAActions.scoreAction(laby, sim, null, dist, distAdversaires, ciblesAdversaires,
                casesInteret, action, memoire, dernierMouvement, inventaire, joueur.getX(), joueur.getY(),
                cibleReference, parametres, acceleration);

            // Petit coup d'avance : regarde la meilleure action suivante (profondeur 2)
            Inventaire invApres = inventaire.copie();
            // Applique les gains/consos de bonus
            invApres.appliquer(sim);
            double suite = IAActions.meilleurApres(etat, idJoueur, sim, invApres,
                distAdversaires, casesInteret, parametres, bfsCache);
            score += parametres.coeffProfondeur * suite;

            // Garde une trace des meilleurs coups
            IAActions.enregistrerTop(top, action, score, sim);

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

            // Candidate "exploration" : on privilégie les cases peu visitées
            if (memoire != null && sim.aBouge()) {
                int idx = laby.index(sim.x, sim.y);
                int freq = memoire.compter(idx);
                if (freq < meilleureExploreFreq || (freq == meilleureExploreFreq && score > meilleureExploreScore)) {
                    meilleureExploreFreq = freq;
                    meilleureExploreScore = score;
                    meilleureExplore = action;
                    meilleureExploreSim = sim;
                }
            }
        }

        // Si la meilleure action ne bouge pas, on préfère une action qui bouge.
        // Cela évite de "passer" alors qu'un déplacement est possible.
        if (meilleurSim != null && !meilleurSim.aBouge() && meilleureMouv != null) {
            return new DecisionIA(meilleureMouv, meilleurMouvSim, top);
        }

        // Si on stagne ou qu'on oscille, on force une case moins visitée
        if (memoire != null && meilleureExplore != null) {
            int idxBest = meilleurSim != null ? laby.index(meilleurSim.x, meilleurSim.y) : -1;
            int freqBest = idxBest >= 0 ? memoire.compter(idxBest) : Integer.MAX_VALUE;
            int twoBack = memoire.recent(1);
            boolean oscillation = (twoBack != -1 && idxBest == twoBack);
            if (toursSansPoints >= parametres.toursSansPointsMax || oscillation) {
                if (meilleureExploreFreq < freqBest) {
                    return new DecisionIA(meilleureExplore, meilleureExploreSim, top);
                }
            }
        }

        return new DecisionIA(meilleure, meilleurSim, top);
    }

}
