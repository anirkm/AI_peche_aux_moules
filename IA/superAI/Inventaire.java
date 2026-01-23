package superAI;

/**
 * Inventaire local des bonus disponibles pour le joueur.
 * Mis à jour après chaque simulation et à chaque tour.
 */
class Inventaire {
    private int bonusSaut;
    private int bonusTroisPas;

    // Copie légère pour la simulation
    Inventaire copie() {
        // Utile pour évaluer une action sans modifier l'état réel
        Inventaire inv = new Inventaire();
        inv.bonusSaut = bonusSaut;
        inv.bonusTroisPas = bonusTroisPas;
        return inv;
    }

    int getBonusSaut() {
        return bonusSaut;
    }

    int getBonusTroisPas() {
        return bonusTroisPas;
    }

    void appliquer(ResultatSimulation resultat) {
        // Applique les gains/consommations de bonus
        bonusSaut += resultat.bonusSautGagne;
        bonusSaut -= resultat.bonusSautUtilise;
        if (bonusSaut < 0) {
            bonusSaut = 0;
        }

        bonusTroisPas += resultat.bonusTroisPasGagne;
        bonusTroisPas -= resultat.bonusTroisPasUtilise;
        if (bonusTroisPas < 0) {
            bonusTroisPas = 0;
        }
    }
}
