package superAI;

class IA {
    static Action choisirAction(EtatJeu etat, int idJoueur) {
        Labyrinthe laby = etat.getLabyrinthe();
        Joueur joueur = etat.getJoueur(idJoueur);

        // Distance depuis notre position
        int[] dist = Distances.bfs(laby, joueur.getX(), joueur.getY());

        int meilleureCase = -1;
        double meilleurScore = -1e18;

        // Choix de la moule la plus intéressante
        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                CaseJeu c = laby.getCase(x, y);
                if (!c.estMoule()) {
                    continue;
                }
                int d = dist[laby.index(x, y)];
                if (d >= Distances.INFINI) {
                    continue;
                }
                double score = c.getValeur() / (d + 1.0);
                if (score > meilleurScore) {
                    meilleurScore = score;
                    meilleureCase = laby.index(x, y);
                }
            }
        }

        if (meilleureCase == -1) {
            return Action.simple('C');
        }

        int cibleX = meilleureCase % laby.getLargeur();
        int cibleY = meilleureCase / laby.getLargeur();
        int[] distVersCible = Distances.bfs(laby, cibleX, cibleY);

        return choisirPasVersCible(laby, joueur.getX(), joueur.getY(), distVersCible);
    }

    private static Action choisirPasVersCible(Labyrinthe laby, int x, int y, int[] distVersCible) {
        char meilleur = 'C';
        int meilleurD = distVersCible[laby.index(x, y)];

        // Test des 4 directions
        meilleur = choisirSiMieux(laby, distVersCible, x, y, 'N', meilleur, meilleurD);
        meilleurD = distanceApres(laby, distVersCible, x, y, meilleur, meilleurD);
        meilleur = choisirSiMieux(laby, distVersCible, x, y, 'S', meilleur, meilleurD);
        meilleurD = distanceApres(laby, distVersCible, x, y, meilleur, meilleurD);
        meilleur = choisirSiMieux(laby, distVersCible, x, y, 'E', meilleur, meilleurD);
        meilleurD = distanceApres(laby, distVersCible, x, y, meilleur, meilleurD);
        meilleur = choisirSiMieux(laby, distVersCible, x, y, 'O', meilleur, meilleurD);

        return Action.simple(meilleur);
    }

    private static char choisirSiMieux(Labyrinthe laby, int[] distVersCible,
                                       int x, int y, char dir, char actuel, int distActuelle) {
        int[] d = delta(dir);
        int nx = x + d[0];
        int ny = y + d[1];
        if (!laby.dansBornes(nx, ny) || laby.estMur(nx, ny)) {
            return actuel;
        }
        int dist = distVersCible[laby.index(nx, ny)];
        if (dist < distActuelle) {
            return dir;
        }
        return actuel;
    }

    private static int distanceApres(Labyrinthe laby, int[] distVersCible,
                                     int x, int y, char dir, int fallback) {
        int[] d = delta(dir);
        int nx = x + d[0];
        int ny = y + d[1];
        if (!laby.dansBornes(nx, ny) || laby.estMur(nx, ny)) {
            return fallback;
        }
        return distVersCible[laby.index(nx, ny)];
    }

    private static int[] delta(char dir) {
        switch (dir) {
            case 'N':
                return new int[]{0, -1};
            case 'S':
                return new int[]{0, 1};
            case 'E':
                return new int[]{1, 0};
            case 'O':
                return new int[]{-1, 0};
            default:
                return new int[]{0, 0};
        }
    }
}
