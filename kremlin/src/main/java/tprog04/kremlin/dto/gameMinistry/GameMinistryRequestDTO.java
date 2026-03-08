package tprog04.kremlin.dto.gameMinistry;

import lombok.Data;

@Data
public class GameMinistryRequestDTO {
    private Long ministryID;
    private Long gameID;
    private Long ministerID;
    private boolean isVacant;
    private int purgeModifier;
}
