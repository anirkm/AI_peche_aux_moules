package superAI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSuperAI {
    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            afficherAide();
            return;
        }
        if (args.length < 3) {
            System.out.println("Utilisation: java ClientSuperAI <ip> <port> <nomEquipe> [options]");
            System.out.println("Astuce: java ClientSuperAI -h");
            return;
        }

        // Paramètres de connexion
        String adresse = args[0];
        int port = Integer.parseInt(args[1]);
        String nomEquipe = args[2];
        ParametresIA parametres = ParametresIA.depuisArgs(args, 3);
        Journal journal = Journal.depuisArgs(args, 3);

        try {
            Socket socket = new Socket(adresse, port);
            BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter sortie = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Envoi du nom d'équipe
            sortie.println(nomEquipe);

            String ligne = entree.readLine();
            if (ligne == null) {
                socket.close();
                return;
            }
            // Numéro de joueur envoyé par le serveur
            int idJoueur = Integer.parseInt(ligne.trim());

            Inventaire inventaire = new Inventaire();
            char dernierMouvement = 'C';
            MemoirePositions memoire = new MemoirePositions(6);
            MemoireCible memoireCible = new MemoireCible();
            int toursSansPoints = 0;

            while (true) {
                String message = entree.readLine();
                if (message == null || "FIN".equals(message)) {
                    break;
                }

                // Décodage de l'état du jeu
                EtatJeu etat = EtatJeu.depuisMessage(message);
                // Choix du coup
                DecisionIA decision = choisirCoup(etat, idJoueur, inventaire, parametres, dernierMouvement,
                    memoire, memoireCible, toursSansPoints);
                Action action = decision.action;

                // Mise à jour locale de l'inventaire
                Joueur joueur = etat.getJoueur(idJoueur);
                ResultatSimulation sim = MoteurSimulation.appliquer(etat, joueur, inventaire, action);
                inventaire.appliquer(sim);
                journal.noterTour(etat, joueur, action, sim, decision.topActions, message);
                dernierMouvement = action.dernierMouvement();
                memoire.ajouter(etat.getLabyrinthe().index(sim.x, sim.y));
                if (sim.points > 0) {
                    toursSansPoints = 0;
                } else {
                    toursSansPoints++;
                }

                sortie.println(action.versCommande());
            }

            journal.fermer();
            socket.close();
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static DecisionIA choisirCoup(EtatJeu etat, int idJoueur, Inventaire inventaire,
                                          ParametresIA parametres, char dernierMouvement,
                                          MemoirePositions memoire, MemoireCible memoireCible,
                                          int toursSansPoints) {
        // Décision basée sur la valeur, les distances et les bonus
        return IA.choisirAction(etat, idJoueur, inventaire, parametres, dernierMouvement,
            memoire, memoireCible, toursSansPoints);
    }

    private static void afficherAide() {
        ParametresIA defauts = new ParametresIA();
        System.out.println("Utilisation:");
        System.out.println("  java ClientSuperAI <ip> <port> <nomEquipe> [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  cle=valeur   (ex: modePlan=1 nbCiblesPlan=8 profondeurPlan=4)");
        System.out.println("  log          active un log simple");
        System.out.println("  logBrut=1    ajoute le message brut du serveur");
        System.out.println("  logFichier=CHEMIN");
        System.out.println();
        System.out.println("Parametres (defauts):");
        System.out.println("  valeurSaut=" + defauts.valeurSaut);
        System.out.println("  valeurTroisPas=" + defauts.valeurTroisPas);
        System.out.println("  penaliteUtilisationSaut=" + defauts.penaliteUtilisationSaut);
        System.out.println("  penaliteUtilisationTroisPas=" + defauts.penaliteUtilisationTroisPas);
        System.out.println("  penaliteDistance=" + defauts.penaliteDistance);
        System.out.println("  coeffProfondeur=" + defauts.coeffProfondeur);
        System.out.println("  penaliteImmobile=" + defauts.penaliteImmobile);
        System.out.println("  penaliteRetour=" + defauts.penaliteRetour);
        System.out.println("  penaliteBoucle=" + defauts.penaliteBoucle);
        System.out.println("  verrouillageCible=" + defauts.verrouillageCible);
        System.out.println("  seuilChangementCible=" + defauts.seuilChangementCible);
        System.out.println("  modeHybride=" + defauts.modeHybride);
        System.out.println("  modeCompromis=" + defauts.modeCompromis);
        System.out.println("  margeCompromis=" + defauts.margeCompromis);
        System.out.println("  margeCompromisFin=" + defauts.margeCompromisFin);
        System.out.println("  modePlan=" + defauts.modePlan);
        System.out.println("  nbCiblesPlan=" + defauts.nbCiblesPlan);
        System.out.println("  profondeurPlan=" + defauts.profondeurPlan);
        System.out.println("  seuilAcceleration=" + defauts.seuilAcceleration);
        System.out.println("  seuilRarete=" + defauts.seuilRarete);
        System.out.println("  toursSansPointsMax=" + defauts.toursSansPointsMax);
        System.out.println("  gainDistanceBonusMin=" + defauts.gainDistanceBonusMin);
        System.out.println("  gainDistanceBonusMinAccel=" + defauts.gainDistanceBonusMinAccel);
        System.out.println();
        System.out.println("Exemple:");
        System.out.println("  java ClientSuperAI 127.0.0.1 1337 MonEquipe log");
        System.out.println("  java ClientSuperAI 127.0.0.1 1337 MonEquipe log modePlan=1 nbCiblesPlan=8 profondeurPlan=4");
    }
}
