package superAI;

/**
 * Mémoire circulaire des dernières positions.
 * Sert à pénaliser les boucles courtes.
 */
class MemoirePositions {
    private final int[] buffer;
    private int taille;
    private int index;

    MemoirePositions(int capacite) {
        // Buffer circulaire des dernières positions visitées
        this.buffer = new int[capacite];
        this.taille = 0;
        this.index = 0;
    }

    void ajouter(int idx) {
        buffer[index] = idx;
        index = (index + 1) % buffer.length;
        if (taille < buffer.length) {
            taille++;
        }
    }

    boolean contient(int idx) {
        return compter(idx) > 0;
    }

    int compter(int idx) {
        int count = 0;
        for (int i = 0; i < taille; i++) {
            if (buffer[i] == idx) {
                count++;
            }
        }
        return count;
    }

    int recent(int offset) {
        // offset=0 => dernière position, offset=1 => avant-dernière, etc.
        if (offset < 0 || offset >= taille) {
            return -1;
        }
        int pos = index - 1 - offset;
        if (pos < 0) {
            pos += buffer.length;
        }
        return buffer[pos];
    }
}
