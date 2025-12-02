package superAI;

class CaseJeu {
    enum Type {
        MUR,
        SOL,
        SAUT,
        TROIS_PAS,
        MOULE
    }

    private Type type;
    private int valeur;

    CaseJeu(Type type, int valeur) {
        this.type = type;
        this.valeur = valeur;
    }

    Type getType() {
        return type;
    }

    int getValeur() {
        return valeur;
    }

    void setType(Type type) {
        this.type = type;
    }

    void setValeur(int valeur) {
        this.valeur = valeur;
    }

    boolean estMur() {
        return type == Type.MUR;
    }

    boolean estMoule() {
        return type == Type.MOULE;
    }
}
