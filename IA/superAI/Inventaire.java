package superAI;

class Inventaire {
    private int bonusSaut;
    private int bonusTroisPas;

    int getBonusSaut() {
        return bonusSaut;
    }

    int getBonusTroisPas() {
        return bonusTroisPas;
    }

    void appliquer(ResultatSimulation resultat) {
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
