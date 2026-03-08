package tprog04.kremlin.mapper;

import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tprog04.kremlin.dto.trial.TrialResponseDTO;
import tprog04.kremlin.models.Trial;
import tprog04.kremlin.services.game.trial.TrialService;

@Component
public class TrialMapper {

    @Autowired
    private TrialVoteMapper voteMapper;
    @Autowired
    private TrialService trialService;

    public TrialResponseDTO toDto(Trial trial) {

        TrialResponseDTO dto = new TrialResponseDTO();

        dto.setId(trial.getId());
        dto.setStatus(trial.getStatus());
        dto.setResult(trial.getResult());

        dto.setAccusedGamePoliticoID(trial.getAccused().getId());
        dto.setProsecutorGamePoliticoID(trial.getProsecutor().getId());
        dto.setTurn(trial.getTurn());

        if (!trial.getVotes().isEmpty()) {
            dto.setVotes(
                trial.getVotes().stream()
                                .map(voteMapper::toDto)
                                .collect(Collectors.toList())
            );
            dto.setAllVotesCast(this.trialService.allVotesCast(trial.getGame()));
        } else {
            dto.setVotes(null);
            dto.setAllVotesCast(false);
        }

        return dto;
    }
}
