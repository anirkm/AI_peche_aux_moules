package superAI;

/**
 * Données minimales d'un joueur côté client.
 * Le serveur ne fournit que position + id.
 */
class Joueur {
    private final int id;
    private int x;
    private int y;
    private int bonusSaut;
    private int bonusTroisPas;

    Joueur(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    int getId() {
        return id;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    int getBonusSaut() {
        return bonusSaut;
    }

    int getBonusTroisPas() {
        return bonusTroisPas;
    }

    void ajouterBonusSaut() {
        bonusSaut++;
    }

    void ajouterBonusTroisPas() {
        bonusTroisPas++;
    }

    void consommerBonusSaut() {
        if (bonusSaut > 0) {
            bonusSaut--;
        }
    }

    void consommerBonusTroisPas() {
        if (bonusTroisPas > 0) {
            bonusTroisPas--;
        }
    }
}
