package superAI;

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
