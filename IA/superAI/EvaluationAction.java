package superAI;

class EvaluationAction {
    final Action action;
    final double score;
    final ResultatSimulation simulation;

    EvaluationAction(Action action, double score, ResultatSimulation simulation) {
        this.action = action;
        this.score = score;
        this.simulation = simulation;
    }
}
