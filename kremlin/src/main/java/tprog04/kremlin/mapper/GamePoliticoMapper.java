package tprog04.kremlin.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;
import tprog04.kremlin.dto.gamePolitico.GamePoliticoRequestDTO;
import tprog04.kremlin.dto.gamePolitico.GamePoliticoResponseDTO;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.repositories.GameMinistryRepository;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.PoliticoRepository;
import tprog04.kremlin.services.ControlPoliticoService;

@Component
public class GamePoliticoMapper {

    @Autowired
    private PoliticoRepository repoPolitico;
    @Autowired
    private GameRepository repoGame;
    @Autowired
    private GameMinistryRepository repoGameMinistry;
    @Autowired
    private ControlPoliticoService controlService;
    @Autowired
    private PoliticoMapper polMapper;

    public GamePoliticoResponseDTO toDto(GamePolitico gamePolitico) {
        GamePoliticoResponseDTO dto = new GamePoliticoResponseDTO();
        dto.setId(gamePolitico.getId());
        dto.setPoliticoDTO(this.polMapper.toDto(gamePolitico.getPolitico()));
        dto.setGameID(gamePolitico.getGame().getId());
        dto.setGameMinistryID(
            gamePolitico.getGameMinistry() != null 
                                           ? gamePolitico.getGameMinistry().getId()
                                           : null
        );
        dto.setCurrentAge(gamePolitico.getCurrentAge());
        dto.setDamage(gamePolitico.getDamage());
        dto.setInvestigationCount(gamePolitico.getInvestigationCount());
        dto.setInvestigationCountAtPhaseStart(gamePolitico.getInvestigationCountAtPhaseStart());
        dto.setImmuneToInvestigationsUntilTurn(
            gamePolitico.getImmuneToInvestigationsUntilTurn() != null
                            ? gamePolitico.getImmuneToInvestigationsUntilTurn()
                            : null
        );
        dto.setStatus(gamePolitico.getStatus() != null
                                               ? gamePolitico.getStatus()
                                               : GamePoliticoStatus.INACTIVE
        );
        Player controller = this.controlService.getControllingPlayer(gamePolitico.getId());
        if (controller != null) {
            dto.setControllerPlayerID(controller.getId());
        }

        return dto;
    }

    public GamePolitico toEntity(GamePoliticoRequestDTO dto) {
        GamePolitico gamePolitico = new GamePolitico();
        repoPolitico.findById(dto.getPoliticoID()).ifPresent(gamePolitico::setPolitico);
        repoGame.findById(dto.getGameID()).ifPresent(gamePolitico::setGame);
        if (dto.getGameMinistryID() != null) {
            repoGameMinistry.findById(dto.getGameMinistryID())
                            .ifPresent(gamePolitico::setGameMinistry);
        }
        gamePolitico.setCurrentAge(dto.getCurrentAge());
        gamePolitico.setDamage(dto.getDamage());
        gamePolitico.setStatus(dto.getStatus());

        return gamePolitico;
    }
}
