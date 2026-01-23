package superAI;

/**
 * Mémoire de la cible en cours.
 * Permet de verrouiller une cible quelques tours pour éviter les zigzags.
 */
class MemoireCible {
    private int cible = -1;
    private int verrou = 0;
    private double scoreCible = -1e18;

    int getCible() {
        return cible;
    }

    void reset() {
        cible = -1;
        verrou = 0;
        scoreCible = -1e18;
    }

    void mettreAJour(Labyrinthe laby, int meilleure, double scoreMeilleur, double scoreActuel, ParametresIA p) {
        // Met à jour la cible et le verrou en fonction des scores
        if (meilleure == -1) {
            cible = -1;
            verrou = 0;
            scoreCible = -1e18;
            return;
        }

        if (!cibleValide(laby)) {
            // Pas de cible valide => on prend la meilleure immédiatement
            cible = meilleure;
            verrou = p.verrouillageCible;
            scoreCible = scoreMeilleur;
            return;
        }

        if (verrou > 0) {
            // Tant que le verrou est actif, on évite de changer trop souvent
            verrou--;
            if (scoreMeilleur > scoreCible * p.seuilChangementCible) {
                cible = meilleure;
                verrou = p.verrouillageCible;
                scoreCible = scoreMeilleur;
            } else if (scoreActuel > -1e17) {
                // On met à jour le score courant si la cible est toujours valide
                scoreCible = scoreActuel;
            }
            return;
        }

        if (scoreMeilleur > scoreCible * p.seuilChangementCible) {
            // Changement autorisé si l'amélioration est significative
            cible = meilleure;
            verrou = p.verrouillageCible;
            scoreCible = scoreMeilleur;
        } else if (scoreActuel > -1e17) {
            scoreCible = scoreActuel;
        }
    }

    private boolean cibleValide(Labyrinthe laby) {
        if (cible < 0) {
            return false;
        }
        int x = cible % laby.getLargeur();
        int y = cible / laby.getLargeur();
        CaseJeu.Type t = laby.getCase(x, y).getType();
        return t == CaseJeu.Type.MOULE || t == CaseJeu.Type.SAUT || t == CaseJeu.Type.TROIS_PAS;
    }
}
