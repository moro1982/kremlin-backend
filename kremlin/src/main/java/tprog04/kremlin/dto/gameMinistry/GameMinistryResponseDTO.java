package tprog04.kremlin.dto.gameMinistry;

import lombok.Data;
import tprog04.kremlin.dto.ministry.MinistryResponseDTO;

@Data
public class GameMinistryResponseDTO {
    private Long id;
    private MinistryResponseDTO ministryDTO;
    private Long gameID;
    private Long ministerID;
    private boolean isVacant;
    private int purgeModifier;
}
