package tprog04.kremlin.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tprog04.kremlin.aux_classes.PhaseType;
import tprog04.kremlin.dto.actionInstance.ActionInstanceDTO;
import tprog04.kremlin.models.ActionInstance;
import tprog04.kremlin.repositories.GameMinistryRepository;
import tprog04.kremlin.repositories.GamePoliticoRepository;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.TrialRepository;

@Component
public class ActionMapper {

    @Autowired
    private GameRepository repoGame;
    @Autowired
    private GamePoliticoRepository repoGamePolitico;
    @Autowired
    private GameMinistryRepository repoGameMinistry;
    @Autowired
    private TrialRepository repoTrial;

    public ActionInstanceDTO toDTO(ActionInstance action) {
        if (action == null) {
            return null;
        }
        ActionInstanceDTO dto = new ActionInstanceDTO();
        dto.setId(action.getId());
        dto.setGameID(action.getGame().getId());
        dto.setActorID(action.getActor().getId());
        dto.setType(action.getType());
        dto.setStatus(action.getStatus());
        dto.setCreatedAt(action.getCreatedAt());
        dto.setTurn(action.getTurn());
        dto.setPhase(PhaseType.fromOrder(action.getPhase()));
        dto.setPriority(action.getPriority());
        dto.setResolved(action.isResolved());
        dto.setTargetGamePoliticoID(action.getTargetGamePolitico().getId());
        if (action.getInfluencePoints() != null && 
            action.getInfluencePoints() != 0
        ) {
            dto.setInfluencePoints(action.getInfluencePoints());
        }
        if (action.getTrial() != null) {
            dto.setTrialID(action.getTrial().getId());
        }
        if (action.getTrialVoteValue() != null) {
            dto.setTrialVoteValue(action.getTrialVoteValue());
        }
        if (action.getTargetGameMinistry() != null) {
            dto.setTargetGameMinistryID(action.getTargetGameMinistry().getId());
        }
        if (action.getActingGamePolitico() != null) {
            dto.setActingGamePoliticoID(action.getActingGamePolitico().getId());
        }

        return dto;
    }

    public ActionInstance toEntity(ActionInstanceDTO dto) {
        ActionInstance action = new ActionInstance();
        action.setGame(this.repoGame.findById(dto.getGameID()).orElseThrow());
        action.setType(dto.getType());
        action.setStatus(dto.getStatus());
        action.setCreatedAt(dto.getCreatedAt());
        action.setTurn(action.getGame().getCurrentTurn());
        action.setPhase(action.getGame().getCurrentPhase());
        action.setPriority(dto.getPriority());
        action.setResolved(dto.isResolved());
        action.setTargetGamePolitico(
            this.repoGamePolitico.findById(dto.getTargetGamePoliticoID())
                                 .orElse(null)
        );
        if (dto.getInfluencePoints() != null && 
            dto.getInfluencePoints() != 0
        ) {
            action.setInfluencePoints(dto.getInfluencePoints());
        }
        if (dto.getTrialID() != null) {
            action.setTrial(
                this.repoTrial.findById(dto.getTrialID())
                              .orElse(null)
            );
        }
        if (dto.getTrialVoteValue() != null) {
            action.setTrialVoteValue(dto.getTrialVoteValue());
        }
        if (dto.getTargetGameMinistryID() != null) {
            action.setTargetGameMinistry(
                this.repoGameMinistry.findById(dto.getTargetGameMinistryID())
                                     .orElse(null)
            );
        }
        if (dto.getActingGamePoliticoID() != null) {
            action.setActingGamePolitico(
                this.repoGamePolitico.findById(dto.getActingGamePoliticoID())
                                     .orElse(null)
            );
        }

        return action;
    }
}
