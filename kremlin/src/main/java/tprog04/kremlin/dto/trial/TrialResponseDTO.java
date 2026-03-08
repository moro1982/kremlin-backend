package tprog04.kremlin.dto.trial;

import java.util.List;
import lombok.Data;
import tprog04.kremlin.aux_classes.TrialResult;
import tprog04.kremlin.aux_classes.TrialStatus;

@Data
public class TrialResponseDTO {
    
    private Long id;
    private TrialStatus status;
    private TrialResult result;

    private Long accusedGamePoliticoID;
    private Long prosecutorGamePoliticoID;

    private Integer turn;
    private List<TrialVoteDTO> votes;
    private boolean allVotesCast;
}
