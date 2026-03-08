package tprog04.kremlin.aux_classes;

public enum ActionStatus {
    ANNOUNCED,          // Declared action, waiting for resolution
    CANCELLED,          // Cancelled after announced
    RESOLVED,           // Succesfully executed
    FAILED              // Failed during resolution
}
