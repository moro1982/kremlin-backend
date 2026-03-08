package tprog04.kremlin.dto.actionInstance;

import java.time.LocalDateTime;
import lombok.Data;
import tprog04.kremlin.aux_classes.ActionStatus;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.aux_classes.PhaseType;
import tprog04.kremlin.aux_classes.TrialVoteValue;

@Data
public class ActionInstanceDTO {
    private Long id;
    private Long gameID;
    private Long actorID;
    private ActionType type;
    private ActionStatus status;
    private LocalDateTime createdAt;
    private int turn;
    private PhaseType phase;
    private int priority;
    private boolean resolved;
    private Long targetGamePoliticoID;
    // If Influence Declaration
    private Integer influencePoints;
    // If Trial
    private Long trialID;
    // If CAST_TRIAL_VOTE
    private TrialVoteValue trialVoteValue;
    // If Promotion/Demotion/Switch of Politicians
    private Long targetGameMinistryID;
    // If Promotion/Demotion/Switch of Politicians or 
        // Rehabilitation from Siberia or
        // Begin/End Investigation
    private Long actingGamePoliticoID;
}
