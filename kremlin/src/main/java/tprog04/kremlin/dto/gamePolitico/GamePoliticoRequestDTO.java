package tprog04.kremlin.dto.gamePolitico;

import lombok.Data;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;

@Data
public class GamePoliticoRequestDTO {
    private Long politicoID;
    private Long gameID;
    private Long gameMinistryID;
    private int currentAge;
    private int damage;
    private GamePoliticoStatus status;
}
