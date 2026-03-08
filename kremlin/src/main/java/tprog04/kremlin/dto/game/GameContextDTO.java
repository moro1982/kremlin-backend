package tprog04.kremlin.dto.game;

import java.util.List;
import lombok.Data;
import tprog04.kremlin.aux_classes.GameLifeCycleStatus;
import tprog04.kremlin.aux_classes.GameStatus;
import tprog04.kremlin.dto.player.LobbyPlayerDTO;

@Data
public class GameContextDTO {
    private Long gameID;
    private GameLifeCycleStatus lifeCycleStatus;
    private GameStatus status;
    private Integer maxPlayers;
    private List<LobbyPlayerDTO> players;
}
