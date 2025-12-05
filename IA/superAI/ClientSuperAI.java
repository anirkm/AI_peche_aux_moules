package superAI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSuperAI {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Utilisation: java ClientSuperAI <ip> <port> <nomEquipe>");
            System.exit(0);
        }

        // Paramètres de connexion
        String adresse = args[0];
        int port = Integer.parseInt(args[1]);
        String nomEquipe = args[2];

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

            while (true) {
                String message = entree.readLine();
                if (message == null || "FIN".equals(message)) {
                    break;
                }

                // Décodage de l'état du jeu
                EtatJeu etat = EtatJeu.depuisMessage(message);
                // Choix du coup (placeholder pour l'instant)
                String coup = choisirCoup(etat, idJoueur);

                sortie.println(coup);
            }

            socket.close();
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String choisirCoup(EtatJeu etat, int idJoueur) {
        // Étape 3 : on passe par la simulation avec un coup simple par défaut
        Joueur joueur = etat.getJoueur(idJoueur);
        Action action = Action.simple('C');
        MoteurSimulation.appliquer(etat, joueur, action);
        return action.versCommande();
    }
}
