package tprog04.kremlin.dto.trial;

import lombok.Data;
import tprog04.kremlin.aux_classes.TrialVoteValue;

@Data
public class TrialVoteDTO {
    private Long voterGamePoliticoID;
    private TrialVoteValue vote;
    private boolean cancelled;
}
