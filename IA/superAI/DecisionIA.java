package superAI;

/**
 * Résultat final d'un choix d'IA.
 * action = commande envoyée, simulation = résultat local,
 * topActions = top 3 pour debug.
 */
class DecisionIA {
    final Action action;
    final ResultatSimulation simulation;
    final EvaluationAction[] topActions;

    DecisionIA(Action action, ResultatSimulation simulation, EvaluationAction[] topActions) {
        this.action = action;
        this.simulation = simulation;
        this.topActions = topActions;
    }
}
