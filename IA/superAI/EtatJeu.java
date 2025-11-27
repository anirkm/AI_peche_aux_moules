package superAI;

import java.util.Arrays;

class EtatJeu {
    enum TypeCase {
        MUR,
        SOL,
        SAUT,
        TROIS_PAS,
        MOULE
    }

    final int largeur;
    final int hauteur;
    final TypeCase[] type;
    final int[] valeur;
    final int nbJoueurs;
    final int[] joueursX;
    final int[] joueursY;

    private EtatJeu(int largeur, int hauteur, TypeCase[] type, int[] valeur,
                    int nbJoueurs, int[] joueursX, int[] joueursY) {
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.type = type;
        this.valeur = valeur;
        this.nbJoueurs = nbJoueurs;
        this.joueursX = joueursX;
        this.joueursY = joueursY;
    }

    static EtatJeu depuisMessage(String message) {
        // Format: "LxH/structure/infosJoueurs"
        String[] parties = message.split("/");
        if (parties.length != 3) {
            throw new IllegalArgumentException("Message invalide: " + message);
        }

        // Taille du plateau
        String[] taille = parties[0].split("x");
        if (taille.length != 2) {
            throw new IllegalArgumentException("Taille invalide: " + parties[0]);
        }
        int largeur = Integer.parseInt(taille[0]);
        int hauteur = Integer.parseInt(taille[1]);

        // Structure des cases
        String[] cases = parties[1].split("-");
        if (cases.length != largeur * hauteur) {
            throw new IllegalArgumentException("Structure invalide: " + cases.length);
        }

        TypeCase[] type = new TypeCase[cases.length];
        int[] valeur = new int[cases.length];
        for (int i = 0; i < cases.length; i++) {
            String token = cases[i];
            if ("Mu".equals(token)) {
                type[i] = TypeCase.MUR;
            } else if ("So".equals(token)) {
                type[i] = TypeCase.SOL;
            } else if ("Bs".equals(token)) {
                type[i] = TypeCase.SAUT;
            } else if ("Bp".equals(token)) {
                type[i] = TypeCase.TROIS_PAS;
            } else {
                type[i] = TypeCase.MOULE;
                valeur[i] = Integer.parseInt(token);
            }
        }

        // Positions des joueurs
        String[] joueurs = parties[2].split("-");
        int nbJoueurs = Integer.parseInt(joueurs[0]);
        int[] joueursX = new int[nbJoueurs];
        int[] joueursY = new int[nbJoueurs];
        for (int i = 0; i < nbJoueurs; i++) {
            String[] pos = joueurs[i + 1].split(",");
            joueursX[i] = Integer.parseInt(pos[0]);
            joueursY[i] = Integer.parseInt(pos[1]);
        }

        return new EtatJeu(largeur, hauteur, type, valeur, nbJoueurs, joueursX, joueursY);
    }

    int index(int x, int y) {
        return y * largeur + x;
    }

    boolean dansBornes(int x, int y) {
        return x >= 0 && x < largeur && y >= 0 && y < hauteur;
    }

    boolean estMur(int x, int y) {
        return type[index(x, y)] == TypeCase.MUR;
    }

    @Override
    public String toString() {
        return "EtatJeu{" +
            "largeur=" + largeur +
            ", hauteur=" + hauteur +
            ", nbJoueurs=" + nbJoueurs +
            ", joueursX=" + Arrays.toString(joueursX) +
            ", joueursY=" + Arrays.toString(joueursY) +
            '}';
    }
}
