package superAI;

/**
 * Paramètres de l'IA (valeurs par défaut + parsing CLI).
 * Les clés sont passées en ligne de commande sous la forme cle=valeur.
 * Valeurs par défaut calibrées sur le preset G1.
 */
class ParametresIA {
    // Valeurs : importance relative des bonus ramassés
    double valeurSaut = 25.0;
    double valeurTroisPas = 35.0;
    // Coûts : évitent de consommer un bonus pour un petit gain
    double penaliteUtilisationSaut = 10.0;
    double penaliteUtilisationTroisPas = 14.0;
    // Coûts de distance et de comportement
    double penaliteDistance = 1.6;
    double coeffProfondeur = 0.7;
    double penaliteImmobile = 6.0;
    double penaliteRetour = 3.5;
    double penaliteBoucle = 7.0;
    // Mémoire de cible (anti-zigzag)
    int verrouillageCible = 4;
    double seuilChangementCible = 1.3;
    // Modes de décision
    int modeHybride = 1;
    int modeCompromis = 1;
    double margeCompromis = 0.15;
    double margeCompromisFin = 0.05;
    // Planification locale (top-K)
    int modePlan = 1;
    int nbCiblesPlan = 8;
    int profondeurPlan = 4;
    // Accélération (quand on stagne ou en fin de partie)
    int seuilAcceleration = 12;
    int seuilRarete = 3;
    int toursSansPointsMax = 12;
    // Seuils d'usage des bonus
    int gainDistanceBonusMin = 3;
    int gainDistanceBonusMinAccel = 2;

    static ParametresIA depuisArgs(String[] args, int debut) {
        // Parse les options après les 3 premiers arguments (ip, port, équipe)
        ParametresIA p = new ParametresIA();
        for (int i = debut; i < args.length; i++) {
            String token = args[i];
            int eq = token.indexOf('=');
            // On ignore les tokens sans '='
            if (eq <= 0 || eq == token.length() - 1) {
                continue;
            }
            String cle = token.substring(0, eq);
            String val = token.substring(eq + 1);
            try {
                // Toutes les valeurs sont lues comme double puis converties si besoin
                double d = Double.parseDouble(val);
                appliquer(p, cle, d);
            } catch (NumberFormatException ignored) {
                // Valeur ignorée
            }
        }
        return p;
    }

    private static void appliquer(ParametresIA p, String cle, double d) {
        // Mappage des clés CLI -> champs internes
        // La liste ci-dessous correspond aux clés visibles dans l'aide (-h)
        if ("valeurSaut".equals(cle)) {
            p.valeurSaut = d;
        } else if ("valeurTroisPas".equals(cle)) {
            p.valeurTroisPas = d;
        } else if ("penaliteUtilisationSaut".equals(cle)) {
            p.penaliteUtilisationSaut = d;
        } else if ("penaliteUtilisationTroisPas".equals(cle)) {
            p.penaliteUtilisationTroisPas = d;
        } else if ("penaliteDistance".equals(cle)) {
            p.penaliteDistance = d;
        } else if ("coeffProfondeur".equals(cle)) {
            p.coeffProfondeur = d;
        } else if ("penaliteImmobile".equals(cle)) {
            p.penaliteImmobile = d;
        } else if ("penaliteRetour".equals(cle)) {
            p.penaliteRetour = d;
        } else if ("penaliteBoucle".equals(cle)) {
            p.penaliteBoucle = d;
        } else if ("verrouillageCible".equals(cle)) {
            p.verrouillageCible = (int) d;
        } else if ("seuilChangementCible".equals(cle)) {
            p.seuilChangementCible = d;
        } else if ("modeHybride".equals(cle)) {
            p.modeHybride = (int) d;
        } else if ("modeCompromis".equals(cle)) {
            p.modeCompromis = (int) d;
        } else if ("margeCompromis".equals(cle)) {
            p.margeCompromis = d;
        } else if ("margeCompromisFin".equals(cle)) {
            p.margeCompromisFin = d;
        } else if ("modePlan".equals(cle)) {
            p.modePlan = (int) d;
        } else if ("nbCiblesPlan".equals(cle)) {
            p.nbCiblesPlan = (int) d;
        } else if ("profondeurPlan".equals(cle)) {
            p.profondeurPlan = (int) d;
        } else if ("seuilAcceleration".equals(cle)) {
            p.seuilAcceleration = (int) d;
        } else if ("seuilRarete".equals(cle)) {
            p.seuilRarete = (int) d;
        } else if ("toursSansPointsMax".equals(cle)) {
            p.toursSansPointsMax = (int) d;
        } else if ("gainDistanceBonusMin".equals(cle)) {
            p.gainDistanceBonusMin = (int) d;
        } else if ("gainDistanceBonusMinAccel".equals(cle)) {
            p.gainDistanceBonusMinAccel = (int) d;
        }
    }
}
