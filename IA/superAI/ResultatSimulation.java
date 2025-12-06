package superAI;

import java.util.Arrays;

class ResultatSimulation {
    int x;
    int y;
    int points;
    int bonusSautGagne;
    int bonusTroisPasGagne;
    int bonusSautUtilise;
    int bonusTroisPasUtilise;

    // Liste des cases ramassées pendant l'action
    private int[] collectees = new int[8];
    private int nbCollectees = 0;

    ResultatSimulation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void ajouterCaseCollectee(int index) {
        // Évite les doublons si on repasse sur la même case
        for (int i = 0; i < nbCollectees; i++) {
            if (collectees[i] == index) {
                return;
            }
        }
        if (nbCollectees >= collectees.length) {
            collectees = Arrays.copyOf(collectees, collectees.length + 8);
        }
        collectees[nbCollectees++] = index;
    }

    boolean aCollecte(int index) {
        for (int i = 0; i < nbCollectees; i++) {
            if (collectees[i] == index) {
                return true;
            }
        }
        return false;
    }
}
