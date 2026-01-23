package superAI;

/**
 * Grille du jeu.
 * Stocke la taille et la liste linéaire des cases.
 */
class Labyrinthe {
    private final int largeur;
    private final int hauteur;
    private final CaseJeu[] cases;

    Labyrinthe(int largeur, int hauteur, CaseJeu[] cases) {
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.cases = cases;
    }

    static Labyrinthe depuisTokens(int largeur, int hauteur, String[] tokens) {
        // Transforme la chaîne de tokens en cases du labyrinthe
        CaseJeu[] cases = new CaseJeu[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            // Mapping direct du protocole -> type interne
            if ("Mu".equals(token)) {
                cases[i] = new CaseJeu(CaseJeu.Type.MUR, 0);
            } else if ("So".equals(token)) {
                cases[i] = new CaseJeu(CaseJeu.Type.SOL, 0);
            } else if ("Bs".equals(token)) {
                cases[i] = new CaseJeu(CaseJeu.Type.SAUT, 0);
            } else if ("Bp".equals(token)) {
                cases[i] = new CaseJeu(CaseJeu.Type.TROIS_PAS, 0);
            } else {
                // Sinon : c'est une moule avec sa valeur en points
                cases[i] = new CaseJeu(CaseJeu.Type.MOULE, Integer.parseInt(token));
            }
        }
        return new Labyrinthe(largeur, hauteur, cases);
    }

    int getLargeur() {
        return largeur;
    }

    int getHauteur() {
        return hauteur;
    }

    int index(int x, int y) {
        // Conversion (x,y) -> index linéaire
        return y * largeur + x;
    }

    boolean dansBornes(int x, int y) {
        return x >= 0 && x < largeur && y >= 0 && y < hauteur;
    }

    CaseJeu getCase(int x, int y) {
        return cases[index(x, y)];
    }

    boolean estMur(int x, int y) {
        return getCase(x, y).estMur();
    }
}
