package tprog04.kremlin.aux_classes;

public enum GameStatus {
    OPEN,               // Players can join in
    CLOSED,             // No more players allowed
    IN_PROGRESS,        // Running
    PAUSED,             // Temporarily suspended
    CANCELLED           // Aborted
}
