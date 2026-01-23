package superAI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Journal optionnel pour tracer les tours côté client.
 * Utile pour le debug et l'analyse des décisions.
 */
class Journal {
    private final boolean actif;
    private final boolean brut;
    private final BufferedWriter sortie;
    private int tour;

    private Journal(boolean actif, boolean brut, BufferedWriter sortie) {
        this.actif = actif;
        this.brut = brut;
        this.sortie = sortie;
        this.tour = 0;
    }

    static Journal depuisArgs(String[] args, int debut) {
        // Active le journal si "log" est présent dans les options
        boolean actif = false;
        boolean brut = false;
        String fichier = null;

        for (int i = debut; i < args.length; i++) {
            String token = args[i];
            if ("log".equals(token) || "log=1".equals(token) || "log=true".equals(token)) {
                actif = true;
            } else if ("logBrut=1".equals(token) || "logBrut=true".equals(token)) {
                brut = true;
                actif = true;
            } else if (token.startsWith("logFichier=")) {
                fichier = token.substring("logFichier=".length());
                actif = true;
            }
        }

        if (!actif) {
            return new Journal(false, false, null);
        }

        if (fichier == null || fichier.isEmpty()) {
            String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            fichier = "IA/superAI/logs/log-" + date + ".txt";
        }

        try {
            File f = new File(fichier);
            File parent = f.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
            return new Journal(true, brut, bw);
        } catch (Exception e) {
            return new Journal(false, false, null);
        }
    }

    void noterTour(EtatJeu etat, Joueur joueur, Action action, ResultatSimulation sim,
                   EvaluationAction[] topActions, String messageBrut) {
        if (!actif || sortie == null) {
            return;
        }
        try {
            Labyrinthe laby = etat.getLabyrinthe();
            int nbMoules = compter(laby, CaseJeu.Type.MOULE);
            int nbSaut = compter(laby, CaseJeu.Type.SAUT);
            int nbTrois = compter(laby, CaseJeu.Type.TROIS_PAS);

            // Format stable pour l'analyse automatique (tools/analyse_logs.py)
            sortie.write("Tour " + tour + "\n");
            sortie.write("Position: (" + joueur.getX() + "," + joueur.getY() + ")\n");
            sortie.write("Action: " + action.versCommande() + "\n");
            sortie.write("Arrivee: (" + sim.x + "," + sim.y + ")\n");
            sortie.write("Gain points: " + sim.points + "\n");
            sortie.write("Bonus saut +/-: +" + sim.bonusSautGagne + " / -" + sim.bonusSautUtilise + "\n");
            sortie.write("Bonus 3 pas +/-: +" + sim.bonusTroisPasGagne + " / -" + sim.bonusTroisPasUtilise + "\n");
            sortie.write("Restant: moules=" + nbMoules + " saut=" + nbSaut + " troisPas=" + nbTrois + "\n");
            if (topActions != null) {
                for (int i = 0; i < topActions.length; i++) {
                    EvaluationAction ev = topActions[i];
                    if (ev == null) {
                        continue;
                    }
                    // Top 3 actions pour comprendre la décision
                    sortie.write("Top " + (i + 1) + ": " + ev.action.versCommande()
                        + " score=" + formater(ev.score)
                        + " arrivee=(" + ev.simulation.x + "," + ev.simulation.y + ")"
                        + " points=" + ev.simulation.points + "\n");
                }
            }
            if (brut && messageBrut != null) {
                sortie.write("Message brut: " + messageBrut + "\n");
            }
            sortie.write("---\n");
            sortie.flush();
            tour++;
        } catch (Exception ignored) {
            // Pas bloquant
        }
    }

    void fermer() {
        if (!actif || sortie == null) {
            return;
        }
        try {
            sortie.close();
        } catch (Exception ignored) {
            // Rien
        }
    }

    private int compter(Labyrinthe laby, CaseJeu.Type type) {
        int total = 0;
        for (int y = 0; y < laby.getHauteur(); y++) {
            for (int x = 0; x < laby.getLargeur(); x++) {
                if (laby.getCase(x, y).getType() == type) {
                    total++;
                }
            }
        }
        return total;
    }

    private String formater(double score) {
        return String.format(java.util.Locale.ROOT, "%.2f", score);
    }
}
