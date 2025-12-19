package superAI;

class ParametresIA {
    double valeurSaut = 20.0;
    double valeurTroisPas = 30.0;
    double penaliteUtilisationSaut = 8.0;
    double penaliteUtilisationTroisPas = 12.0;
    double penaliteDistance = 1.4;
    double coeffProfondeur = 0.6;
    double penaliteImmobile = 6.0;

    static ParametresIA depuisArgs(String[] args, int debut) {
        ParametresIA p = new ParametresIA();
        for (int i = debut; i < args.length; i++) {
            String token = args[i];
            int eq = token.indexOf('=');
            if (eq <= 0 || eq == token.length() - 1) {
                continue;
            }
            String cle = token.substring(0, eq);
            String val = token.substring(eq + 1);
            try {
                double d = Double.parseDouble(val);
                appliquer(p, cle, d);
            } catch (NumberFormatException ignored) {
                // Valeur ignorée
            }
        }
        return p;
    }

    private static void appliquer(ParametresIA p, String cle, double d) {
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
        }
    }
}
