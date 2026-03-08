package tprog04.kremlin.aux_classes;

public enum GameLifeCycleStatus {
    CREATED,                  // Game created, none or one player
    LOBBY,                    // Awaiting players
    INFLUENCE_ASSIGNMENT,     // Turn 0
    RUNNING,                  // Turn 1+
    FINISHED                  // Game finished
}
