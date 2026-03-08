package tprog04.kremlin.aux_classes;

public enum ActionBlockingStatus {
    NONE,
    AWAITING_PURGE_RESPONSE,
    FAILED_PURGE_BLOCK,
    AWAITING_TRIAL_RESPONSE,
    AWAITING_TRIAL_VOTES,
    AWAITING_CONDEMNATION_RESPONSE
}
