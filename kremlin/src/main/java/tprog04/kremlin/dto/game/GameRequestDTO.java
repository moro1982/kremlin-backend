package tprog04.kremlin.dto.game;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import tprog04.kremlin.aux_classes.ActionBlockingStatus;
import tprog04.kremlin.aux_classes.GameStatus;
import tprog04.kremlin.aux_classes.PhaseExecutionStatus;

@Data
public class GameRequestDTO {
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private Integer currentTurn;
    private Integer currentPhase;
    private boolean finished;
    private PhaseExecutionStatus phaseStatus;
    private Long version;
    private Long updateCounter;
    private ActionBlockingStatus blockingStatus;
    private Integer maxPlayers;
    private GameStatus status;
    private Set<Long> players = new HashSet<>();
    private Set<Long> gamePoliticos = new HashSet<>();
    private Set<Long> readyPlayers = new HashSet<>();
}
