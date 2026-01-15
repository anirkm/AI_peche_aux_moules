package superAI;

import java.util.Arrays;

class Distances { // Parcours en largeur
    static final int INFINI = 1_000_000;
    private static final int[] DX = new int[]{0, 0, 1, -1};
    private static final int[] DY = new int[]{-1, 1, 0, 0};

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

    static int[] bfsAvecBonus(Labyrinthe laby, int startX, int startY, int bonusSaut, int bonusTroisPas) {
        int n = laby.getLargeur() * laby.getHauteur();
        int[] best = new int[n];
        Arrays.fill(best, INFINI);

        if (!laby.dansBornes(startX, startY)) {
            return best;
        }

        int sMax = Math.max(0, bonusSaut);
        int tMax = Math.max(0, bonusTroisPas);
        // Etat = (case, nbSaut, nbTroisPas)
        int states = (sMax + 1) * (tMax + 1) * n;
        int[] dist = new int[states];
        Arrays.fill(dist, INFINI);

        int start = laby.index(startX, startY);
        int startState = indexState(start, sMax, tMax, n, tMax);
        dist[startState] = 0;
        best[start] = 0;

        int[] file = new int[states];
        int tete = 0;
        int queue = 0;
        file[queue++] = startState;

        while (tete < queue) {
            int state = file[tete++];
            int cell = state % n;
            int tmp = state / n;
            int t = tmp % (tMax + 1);
            int s = tmp / (tMax + 1);
            int x = cell % laby.getLargeur();
            int y = cell / laby.getLargeur();
            int nd = dist[state] + 1;

            // Déplacements simples
            for (int dir = 0; dir < 4; dir++) {
                int nx = x + DX[dir];
                int ny = y + DY[dir];
                if (!laby.dansBornes(nx, ny) || laby.estMur(nx, ny)) {
                    continue;
                }
                int nc = laby.index(nx, ny);
                if (nc == cell) {
                    continue;
                }
                int ns = s;
                int nt = t;
                int nstate = indexState(nc, ns, nt, n, tMax);
                if (dist[nstate] == INFINI) {
                    dist[nstate] = nd;
                    file[queue++] = nstate;
                    if (nd < best[nc]) {
                        best[nc] = nd;
                    }
                }
            }

            // Bonus saut
            if (s > 0) {
                for (int dir = 0; dir < 4; dir++) {
                    int nx = x + DX[dir] * 2;
                    int ny = y + DY[dir] * 2;
                    if (!laby.dansBornes(nx, ny) || laby.estMur(nx, ny)) {
                        continue;
                    }
                    int nc = laby.index(nx, ny);
                    if (nc == cell) {
                        continue;
                    }
                    int ns = s - 1;
                    int nt = t;
                    int nstate = indexState(nc, ns, nt, n, tMax);
                    if (dist[nstate] == INFINI) {
                        dist[nstate] = nd;
                        file[queue++] = nstate;
                        if (nd < best[nc]) {
                            best[nc] = nd;
                        }
                    }
                }
            }

            // Bonus trois pas
            if (t > 0) {
                for (int d1 = 0; d1 < 4; d1++) {
                    for (int d2 = 0; d2 < 4; d2++) {
                        for (int d3 = 0; d3 < 4; d3++) {
                            int nx = x;
                            int ny = y;
                            int[] p1 = avancer(laby, nx, ny, d1);
                            nx = p1[0];
                            ny = p1[1];
                            int[] p2 = avancer(laby, nx, ny, d2);
                            nx = p2[0];
                            ny = p2[1];
                            int[] p3 = avancer(laby, nx, ny, d3);
                            nx = p3[0];
                            ny = p3[1];
                            int nc = laby.index(nx, ny);
                            if (nc == cell) {
                                continue;
                            }
                            int ns = s;
                            int nt = t - 1;
                            int nstate = indexState(nc, ns, nt, n, tMax);
                            if (dist[nstate] == INFINI) {
                                dist[nstate] = nd;
                                file[queue++] = nstate;
                                if (nd < best[nc]) {
                                    best[nc] = nd;
                                }
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    static int distanceAvecBonus(Labyrinthe laby, int startX, int startY,
                                 int bonusSaut, int bonusTroisPas, int cibleIdx) {
        int n = laby.getLargeur() * laby.getHauteur();
        if (!laby.dansBornes(startX, startY)) {
            return INFINI;
        }
        int start = laby.index(startX, startY);
        if (start == cibleIdx) {
            return 0;
        }

        int sMax = Math.max(0, bonusSaut);
        int tMax = Math.max(0, bonusTroisPas);
        int states = (sMax + 1) * (tMax + 1) * n;
        int[] dist = new int[states];
        Arrays.fill(dist, INFINI);

        int startState = indexState(start, sMax, tMax, n, tMax);
        dist[startState] = 0;

        int[] file = new int[states];
        int tete = 0;
        int queue = 0;
        file[queue++] = startState;

        while (tete < queue) {
            int state = file[tete++];
            int cell = state % n;
            int tmp = state / n;
            int t = tmp % (tMax + 1);
            int s = tmp / (tMax + 1);
            int x = cell % laby.getLargeur();
            int y = cell / laby.getLargeur();
            int nd = dist[state] + 1;

            // Déplacements simples
            for (int dir = 0; dir < 4; dir++) {
                int nx = x + DX[dir];
                int ny = y + DY[dir];
                if (!laby.dansBornes(nx, ny) || laby.estMur(nx, ny)) {
                    continue;
                }
                int nc = laby.index(nx, ny);
                if (nc == cell) {
                    continue;
                }
                if (nc == cibleIdx) {
                    return nd;
                }
                int nstate = indexState(nc, s, t, n, tMax);
                if (dist[nstate] == INFINI) {
                    dist[nstate] = nd;
                    file[queue++] = nstate;
                }
            }

            // Bonus saut
            if (s > 0) {
                for (int dir = 0; dir < 4; dir++) {
                    int nx = x + DX[dir] * 2;
                    int ny = y + DY[dir] * 2;
                    if (!laby.dansBornes(nx, ny) || laby.estMur(nx, ny)) {
                        continue;
                    }
                    int nc = laby.index(nx, ny);
                    if (nc == cell) {
                        continue;
                    }
                    if (nc == cibleIdx) {
                        return nd;
                    }
                    int nstate = indexState(nc, s - 1, t, n, tMax);
                    if (dist[nstate] == INFINI) {
                        dist[nstate] = nd;
                        file[queue++] = nstate;
                    }
                }
            }

            // Bonus trois pas
            if (t > 0) {
                for (int d1 = 0; d1 < 4; d1++) {
                    for (int d2 = 0; d2 < 4; d2++) {
                        for (int d3 = 0; d3 < 4; d3++) {
                            int nx = x;
                            int ny = y;
                            int[] p1 = avancer(laby, nx, ny, d1);
                            nx = p1[0];
                            ny = p1[1];
                            int[] p2 = avancer(laby, nx, ny, d2);
                            nx = p2[0];
                            ny = p2[1];
                            int[] p3 = avancer(laby, nx, ny, d3);
                            nx = p3[0];
                            ny = p3[1];
                            int nc = laby.index(nx, ny);
                            if (nc == cell) {
                                continue;
                            }
                            if (nc == cibleIdx) {
                                return nd;
                            }
                            int nstate = indexState(nc, s, t - 1, n, tMax);
                            if (dist[nstate] == INFINI) {
                                dist[nstate] = nd;
                                file[queue++] = nstate;
                            }
                        }
                    }
                }
            }
        }

        return INFINI;
    }

    private static int indexState(int cell, int s, int t, int n, int tMax) {
        return ((s * (tMax + 1) + t) * n) + cell;
    }

    private static int[] avancer(Labyrinthe laby, int x, int y, int dir) {
        int nx = x + DX[dir];
        int ny = y + DY[dir];
        if (!laby.dansBornes(nx, ny) || laby.estMur(nx, ny)) {
            return new int[]{x, y};
        }
        return new int[]{nx, ny};
    }
}
