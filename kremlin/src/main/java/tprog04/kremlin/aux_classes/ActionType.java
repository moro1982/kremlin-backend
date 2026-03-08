package tprog04.kremlin.aux_classes;

import java.util.HashSet;
import java.util.Set;

public enum ActionType {
    DECLARE_INFLUENCE(Set.of(1,2,3,4,5,6,7,8), 1),
    SEND_HOSPITAL(Set.of(1), 3),
    EXIT_HOSPITAL(Set.of(1), 3),
    PURGE_ATTEMPT(Set.of(2), 3),
    EXILE_ESCAPE(Set.of(2,3), 2),
    EXILE_RETURN(Set.of(1,2,3,4,5,6,7,8), 2),
    BEGIN_INVESTIGATION(Set.of(3), 3),
    REMOVE_INVESTIGATION(Set.of(3), 3),
    OPEN_TRIAL(Set.of(3), 3),
    CAST_TRIAL_VOTE(Set.of(3), 2),
    CONDEMNATION(Set.of(3), 3),
    NEGATE_CONDEMNATION(Set.of(3), 2),
    NOMINATE_SUCCESOR(Set.of(5), 3),
    PROMOTE_MINISTER(Set.of(6), 3),
    DEMOTE_MINISTER(Set.of(6), 3),
    SWITCH_MINISTER(Set.of(6), 3),
    REHABILITATE_PRISONER(Set.of(7), 3),
    LEADER_WAVE(Set.of(8), 3);

    private Set<Integer> allowedPhases = new HashSet<>();

    private int priority;

    private ActionType(Set<Integer> allowedPhases, int priority) {
        this.allowedPhases = allowedPhases;
        this.priority = priority;
    }

    public Set<Integer> getAllowedPhases() {
        return this.allowedPhases;
    }

    public int getPriority() {
        return this.priority;
    }

    public static Set<ActionType> fromOrder(int order) {
        Set<ActionType> actions = new HashSet<>();
        for (ActionType action : values()) {
            for (int phaseNr : action.getAllowedPhases()) {
                if (phaseNr == order) {
                    actions.add(action);
                }
            }
        }
        return actions;
    }
}
