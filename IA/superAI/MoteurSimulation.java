package superAI;

class MoteurSimulation {
    static ResultatSimulation appliquer(EtatJeu etat, Joueur joueur, Inventaire inventaire, Action action) {
        return appliquer(etat, joueur, inventaire, action, null);
    }

    static ResultatSimulation appliquer(EtatJeu etat, Joueur joueur, Inventaire inventaire,
                                        Action action, ResultatSimulation dejaCollecte) {
        Labyrinthe laby = etat.getLabyrinthe();

        int x = joueur.getX();
        int y = joueur.getY();

        // Résultat de la simulation locale (sans modifier le serveur)
        ResultatSimulation resultat = new ResultatSimulation(x, y);

        switch (action.getType()) {
            case SIMPLE:
                // Un déplacement simple
                int[] pos = deplacerSimple(laby, x, y, action.getD1());
                x = pos[0];
                y = pos[1];
                ramasser(laby, resultat, dejaCollecte, x, y);
                break;
            case SAUT:
                // Bonus saut : 2 cases si la cible est valide
                if (inventaire.getBonusSaut() > 0) {
                    int[] delta = delta(action.getD1());
                    int nx = x + delta[0] * 2;
                    int ny = y + delta[1] * 2;
                    if (laby.dansBornes(nx, ny) && !laby.estMur(nx, ny)) {
                        x = nx;
                        y = ny;
                        ramasser(laby, resultat, dejaCollecte, x, y);
                        resultat.bonusSautUtilise = 1;
                    } else {
                        int[] fallback = deplacerSimple(laby, x, y, action.getD1());
                        x = fallback[0];
                        y = fallback[1];
                        ramasser(laby, resultat, dejaCollecte, x, y);
                    }
                }
                break;
            case TROIS_PAS:
                // Bonus 3 pas : trois déplacements simples
                if (inventaire.getBonusTroisPas() > 0) {
                    int[] p1 = deplacerSimple(laby, x, y, action.getD1());
                    x = p1[0];
                    y = p1[1];
                    ramasser(laby, resultat, dejaCollecte, x, y);

                    int[] p2 = deplacerSimple(laby, x, y, action.getD2());
                    x = p2[0];
                    y = p2[1];
                    ramasser(laby, resultat, dejaCollecte, x, y);

                    int[] p3 = deplacerSimple(laby, x, y, action.getD3());
                    x = p3[0];
                    y = p3[1];
                    ramasser(laby, resultat, dejaCollecte, x, y);

                    resultat.bonusTroisPasUtilise = 1;
                }
                break;
            default:
                break;
        }

        resultat.x = x;
        resultat.y = y;
        return resultat;
    }

    private static int[] deplacerSimple(Labyrinthe laby, int x, int y, char dir) {
        // Ne traverse pas les murs
        int[] d = delta(dir);
        if (d[0] == 0 && d[1] == 0) {
            return new int[]{x, y};
        }
        int nx = x + d[0];
        int ny = y + d[1];
        if (!laby.dansBornes(nx, ny)) {
            return new int[]{x, y};
        }
        if (laby.estMur(nx, ny)) {
            return new int[]{x, y};
        }
        return new int[]{nx, ny};
    }

    private static void ramasser(Labyrinthe laby, ResultatSimulation resultat,
                                 ResultatSimulation dejaCollecte, int x, int y) {
        // Ramassage des éléments sur la case
        CaseJeu c = laby.getCase(x, y);
        int idx = laby.index(x, y);
        if (dejaCollecte != null && dejaCollecte.aCollecte(idx)) {
            return;
        }
        if (c.getType() == CaseJeu.Type.MOULE) {
            resultat.points += c.getValeur();
        } else if (c.getType() == CaseJeu.Type.SAUT) {
            resultat.bonusSautGagne += 1;
        } else if (c.getType() == CaseJeu.Type.TROIS_PAS) {
            resultat.bonusTroisPasGagne += 1;
        } else {
            return;
        }
        resultat.ajouterCaseCollectee(idx);
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
