package superAI;

import java.util.Arrays;

class Distances { // Parcours en largeur
    static final int INFINI = 1_000_000;

    static int[] bfs(Labyrinthe laby, int startX, int startY) {
        // Distances BFS depuis une case
        int n = laby.getLargeur() * laby.getHauteur();
        int[] dist = new int[n];
        Arrays.fill(dist, INFINI);

        if (!laby.dansBornes(startX, startY)) {
            return dist;
        }

        int start = laby.index(startX, startY);
        dist[start] = 0;

        int[] file = new int[n];
        int tete = 0;
        int queue = 0;
        file[queue++] = start;

        while (tete < queue) {
            int idx = file[tete++];
            int x = idx % laby.getLargeur();
            int y = idx / laby.getLargeur();
            int nd = dist[idx] + 1;

            // Voisin est
            int nx = x + 1;
            int ny = y;
            if (laby.dansBornes(nx, ny)) {
                int ni = laby.index(nx, ny);
                if (dist[ni] == INFINI && !laby.estMur(nx, ny)) {
                    dist[ni] = nd;
                    file[queue++] = ni;
                }
            }

            // Voisin ouest
            nx = x - 1;
            ny = y;
            if (laby.dansBornes(nx, ny)) {
                int ni = laby.index(nx, ny);
                if (dist[ni] == INFINI && !laby.estMur(nx, ny)) {
                    dist[ni] = nd;
                    file[queue++] = ni;
                }
            }

            // Voisin sud
            nx = x;
            ny = y + 1;
            if (laby.dansBornes(nx, ny)) {
                int ni = laby.index(nx, ny);
                if (dist[ni] == INFINI && !laby.estMur(nx, ny)) {
                    dist[ni] = nd;
                    file[queue++] = ni;
                }
            }

            // Voisin nord
            nx = x;
            ny = y - 1;
            if (laby.dansBornes(nx, ny)) {
                int ni = laby.index(nx, ny);
                if (dist[ni] == INFINI && !laby.estMur(nx, ny)) {
                    dist[ni] = nd;
                    file[queue++] = ni;
                }
            }
        }

        return dist;
    }
}
