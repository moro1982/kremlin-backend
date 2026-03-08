package tprog04.kremlin.mapper;

import org.springframework.stereotype.Component;
import tprog04.kremlin.dto.trial.TrialVoteDTO;
import tprog04.kremlin.models.TrialVote;

@Component
public class TrialVoteMapper {

    public TrialVoteDTO toDto(TrialVote vote) {
        
        TrialVoteDTO dto = new TrialVoteDTO();

        dto.setVoterGamePoliticoID(vote.getVoter().getId());
        dto.setVote(vote.getVote());
        dto.setCancelled(vote.isCancelled());

        return dto;
    }

}
