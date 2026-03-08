package tprog04.kremlin.dto.gamePolitico;

import lombok.Data;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;
import tprog04.kremlin.dto.politico.PoliticoResponseDTO;

@Data
public class GamePoliticoResponseDTO {
    private Long id;
    private PoliticoResponseDTO politicoDTO;
    private Long gameID;
    private Long gameMinistryID;
    private int currentAge;
    private int damage;
    private int investigationCount;
    private int investigationCountAtPhaseStart;
    private Integer immuneToInvestigationsUntilTurn;
    private GamePoliticoStatus status;
    private Long controllerPlayerID;
}
