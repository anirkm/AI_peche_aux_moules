package superAI;

class EtatJeu {
    private final Labyrinthe labyrinthe;
    private final Joueur[] joueurs;

    private EtatJeu(Labyrinthe labyrinthe, Joueur[] joueurs) {
        this.labyrinthe = labyrinthe;
        this.joueurs = joueurs;
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
        Labyrinthe labyrinthe = Labyrinthe.depuisTokens(largeur, hauteur, cases);

        // Positions des joueurs
        String[] joueurs = parties[2].split("-");
        int nbJoueurs = Integer.parseInt(joueurs[0]);
        Joueur[] liste = new Joueur[nbJoueurs];
        for (int i = 0; i < nbJoueurs; i++) {
            String[] pos = joueurs[i + 1].split(",");
            int x = Integer.parseInt(pos[0]);
            int y = Integer.parseInt(pos[1]);
            liste[i] = new Joueur(i, x, y);
        }

        return new EtatJeu(labyrinthe, liste);
    }

    Labyrinthe getLabyrinthe() {
        return labyrinthe;
    }

    int getNbJoueurs() {
        return joueurs.length;
    }

    Joueur getJoueur(int id) {
        return joueurs[id];
    }
}
