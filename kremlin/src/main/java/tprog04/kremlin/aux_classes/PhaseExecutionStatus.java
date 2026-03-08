package tprog04.kremlin.aux_classes;

public enum PhaseExecutionStatus {
    NONE,                   // Game not started yet.
    WAITING_TO_BEGIN,       // Awaiting players to mark "ready".
    OPEN_FOR_ACTIONS,       // Awaiting players to declare actions and mark "ready".
    RESOLVING_ACTIONS,      // Resolving pending actions.
    FINISHED                // Phase ended.
}
