package superAI;

class Action {
    enum Type {
        SIMPLE,
        SAUT,
        TROIS_PAS
    }

    // Représente une action complète envoyée au serveur
    private final Type type;
    private final char d1;
    private final char d2;
    private final char d3;

    private Action(Type type, char d1, char d2, char d3) {
        this.type = type;
        this.d1 = d1;
        this.d2 = d2;
        this.d3 = d3;
    }

    static Action simple(char direction) {
        return new Action(Type.SIMPLE, direction, 'C', 'C');
    }

    static Action saut(char direction) {
        return new Action(Type.SAUT, direction, 'C', 'C');
    }

    static Action troisPas(char d1, char d2, char d3) {
        return new Action(Type.TROIS_PAS, d1, d2, d3);
    }

    Type getType() {
        return type;
    }

    char getD1() {
        return d1;
    }

    char getD2() {
        return d2;
    }

    char getD3() {
        return d3;
    }

    String versCommande() {
        // Format exact attendu par le serveur
        if (type == Type.SIMPLE) {
            return String.valueOf(d1);
        }
        if (type == Type.SAUT) {
            return "Bs-" + d1;
        }
        return "Bp-" + d1 + "-" + d2 + "-" + d3;
    }

    char dernierMouvement() {
        if (type == Type.TROIS_PAS) {
            if (d3 != 'C') {
                return d3;
            }
            if (d2 != 'C') {
                return d2;
            }
            return d1;
        }
        return d1;
    }
}
