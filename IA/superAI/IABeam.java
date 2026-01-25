package superAI;

import java.util.Arrays;

/**
 * Recherche par faisceau (beam search) pour tester des séquences d'actions.
 */
class IABeam {
    static DecisionIA decisionBeam(EtatJeu etat, int idJoueur, Inventaire inventaire,
                                   int[] distAdversaires, ParametresIA parametres,
                                   char dernierMouvement, MemoirePositions memoire,
                                   int cibleReference, int[] ciblesAdversaires,
                                   boolean acceleration, int[] casesInteret,
                                   int[][] bfsCache) {
        // Recherche par faisceau sur des séquences d'actions
        // Idée : on garde seulement les meilleurs chemins à chaque profondeur.
        int profondeur = Math.max(1, parametres.profondeurBeam);
        int largeur = Math.max(1, parametres.largeurBeam);
        Labyrinthe laby = etat.getLabyrinthe();
        Joueur joueur = etat.getJoueur(idJoueur);

        Action[] actionsInit = IAActions.actionsPossibles(laby, joueur, inventaire);
        if (actionsInit.length == 0) {
            return null;
        }

        EvaluationAction[] top = new EvaluationAction[3];
        // Étape 1 : beam initial sur les actions immédiates
        // On initialise la mémoire locale pour éviter de revenir sur place trop vite.
        BeamNode[] candidats = new BeamNode[actionsInit.length];
        int count = 0;
        int[] baseRecent = new int[6];
        baseRecent[0] = laby.index(joueur.getX(), joueur.getY());
        int baseCount = 1;
        int distStart = cibleReference != -1
            ? Distances.distanceAvecBonus(laby, joueur.getX(), joueur.getY(),
                inventaire.getBonusSaut(), inventaire.getBonusTroisPas(), cibleReference)
            : Distances.INFINI;
        for (Action action : actionsInit) {
            ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
            Inventaire invApres = inventaire.copie();
            invApres.appliquer(sim);
            int[] dist = Distances.bfsCached(laby, sim.x, sim.y, bfsCache);
            double score = IAActions.scoreAction(laby, sim, null, dist, distAdversaires, ciblesAdversaires,
                casesInteret, action, memoire, dernierMouvement, inventaire, joueur.getX(), joueur.getY(),
                cibleReference, parametres, acceleration);
            int localFreq = countRecent(baseRecent, baseCount, laby.index(sim.x, sim.y));
            if (localFreq > 0) {
                score -= parametres.penaliteBoucle * localFreq;
            }
            int distCible = cibleReference != -1
                ? Distances.distanceAvecBonus(laby, sim.x, sim.y,
                    invApres.getBonusSaut(), invApres.getBonusTroisPas(), cibleReference)
                : Distances.INFINI;
            score += scoreProgression(distStart, distCible, parametres);
            IAActions.enregistrerTop(top, action, score, sim);
            int[] nextRecent = pushRecent(baseRecent, baseCount, laby.index(sim.x, sim.y));
            int nextCount = nextCount(baseCount, baseRecent.length);
            candidats[count++] = new BeamNode(action, sim, sim, invApres, score,
                action.dernierMouvement(), nextRecent, nextCount, distCible);
        }

        // Garder les meilleurs noeuds
        BeamNode[] beam = topBeam(candidats, count, largeur);
        double poids = parametres.coeffProfondeur;
        int maxActions = 1 + 4 + 4 + 64;

        for (int depth = 2; depth <= profondeur; depth++) {
            // Étape suivante : expansion de chaque noeud
            BeamNode[] suivants = new BeamNode[Math.max(beam.length, 1) * maxActions];
            int nextCount = 0;
            for (BeamNode node : beam) {
                Joueur virtuel = new Joueur(idJoueur, node.lastSim.x, node.lastSim.y);
                Action[] actions = IAActions.actionsPossibles(laby, virtuel, node.inventaire);
                for (Action action : actions) {
                    // Simulation locale à partir du noeud courant
                    ResultatSimulation sim = MoteurSimulation.appliquer(etat, virtuel, node.inventaire, action, node.lastSim);
                    Inventaire invApres = node.inventaire.copie();
                    invApres.appliquer(sim);
                    int[] dist = Distances.bfsCached(laby, sim.x, sim.y, bfsCache);
                    double stepScore = IAActions.scoreAction(laby, sim, node.lastSim, dist, distAdversaires, ciblesAdversaires,
                        casesInteret, action, memoire, node.dernierMouvement, node.inventaire, node.lastSim.x, node.lastSim.y,
                        cibleReference, parametres, acceleration);
                    int idx = laby.index(sim.x, sim.y);
                    int localFreq = countRecent(node.recent, node.recentCount, idx);
                    if (localFreq > 0) {
                        stepScore -= parametres.penaliteBoucle * localFreq;
                    }
                    int distCible = cibleReference != -1
                        ? Distances.distanceAvecBonus(laby, sim.x, sim.y,
                            invApres.getBonusSaut(), invApres.getBonusTroisPas(), cibleReference)
                        : Distances.INFINI;
                    stepScore += scoreProgression(node.distCible, distCible, parametres);
                    // Somme pondérée (discount)
                    double total = node.score + poids * stepScore;
                    if (nextCount < suivants.length) {
                        int[] nextRecent = pushRecent(node.recent, node.recentCount, idx);
                        int nextRecentCount = nextCount(node.recentCount, node.recent.length);
                        suivants[nextCount++] = new BeamNode(node.firstAction, node.firstSim, sim, invApres,
                            total, action.dernierMouvement(), nextRecent, nextRecentCount, distCible);
                    }
                }
            }
            if (nextCount == 0) {
                break;
            }
            // On garde uniquement les meilleurs noeuds
            beam = topBeam(suivants, nextCount, largeur);
            poids *= parametres.coeffProfondeur;
        }

        if (beam.length == 0) {
            return null;
        }
        // On retourne la première action du meilleur chemin
        BeamNode best = beam[0];
        return new DecisionIA(best.firstAction, best.firstSim, top);
    }

    private static BeamNode[] topBeam(BeamNode[] nodes, int count, int k) {
        int m = Math.min(k, count);
        // Tri partiel décroissant sur le score
        // (k petit, tri simple suffisant)
        for (int i = 0; i < m; i++) {
            int best = i;
            for (int j = i + 1; j < count; j++) {
                if (nodes[j].score > nodes[best].score) {
                    best = j;
                }
            }
            if (best != i) {
                BeamNode tmp = nodes[i];
                nodes[i] = nodes[best];
                nodes[best] = tmp;
            }
        }
        BeamNode[] top = new BeamNode[m];
        System.arraycopy(nodes, 0, top, 0, m);
        return top;
    }

    private static int countRecent(int[] recent, int count, int idx) {
        // Compte le nombre d'occurrences d'une case récente
        int c = 0;
        for (int i = 0; i < count; i++) {
            if (recent[i] == idx) {
                c++;
            }
        }
        return c;
    }

    private static int[] pushRecent(int[] recent, int count, int idx) {
        // Garde une petite trace des dernières cases visitées
        int max = recent.length;
        int[] next = Arrays.copyOf(recent, max);
        if (count < max) {
            next[count] = idx;
        } else {
            System.arraycopy(next, 1, next, 0, max - 1);
            next[max - 1] = idx;
        }
        return next;
    }

    private static int nextCount(int count, int max) {
        return count < max ? count + 1 : max;
    }

    private static double scoreProgression(int dAvant, int dApres, ParametresIA parametres) {
        // Bonus/malus si la distance vers la cible s'améliore ou se dégrade
        if (dAvant >= Distances.INFINI || dApres >= Distances.INFINI) {
            return 0.0;
        }
        int gain = dAvant - dApres;
        if (gain <= 0) {
            return -parametres.penaliteDistance * 1.5;
        }
        return gain * parametres.penaliteDistance * 1.3;
    }

    private static class BeamNode {
        // Noeud du beam : on garde l'action initiale + l'état courant.
        final Action firstAction;
        final ResultatSimulation firstSim;
        final ResultatSimulation lastSim;
        final Inventaire inventaire;
        final double score;
        final char dernierMouvement;
        final int[] recent;
        final int recentCount;
        final int distCible;

        BeamNode(Action firstAction, ResultatSimulation firstSim, ResultatSimulation lastSim,
                 Inventaire inventaire, double score, char dernierMouvement,
                 int[] recent, int recentCount, int distCible) {
            this.firstAction = firstAction;
            this.firstSim = firstSim;
            this.lastSim = lastSim;
            this.inventaire = inventaire;
            this.score = score;
            this.dernierMouvement = dernierMouvement;
            this.recent = recent;
            this.recentCount = recentCount;
            this.distCible = distCible;
        }
    }
}
