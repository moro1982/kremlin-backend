package tprog04.kremlin.dto.game;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import tprog04.kremlin.aux_classes.ActionBlockingStatus;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.aux_classes.GameLifeCycleStatus;
import tprog04.kremlin.aux_classes.MinistryEnum;
import tprog04.kremlin.aux_classes.PhaseExecutionStatus;
import tprog04.kremlin.aux_classes.PhaseType;
import tprog04.kremlin.dto.actionInstance.ActionInstanceDTO;
import tprog04.kremlin.dto.gameMinistry.GameMinistryResponseDTO;
import tprog04.kremlin.dto.gamePolitico.GamePoliticoResponseDTO;
import tprog04.kremlin.dto.player.PlayerResponseDTO;
import tprog04.kremlin.dto.trial.TrialResponseDTO;

@Data
public class GameResponseDTO {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private Integer currentTurn;
    private PhaseType currentPhase;
    private PhaseExecutionStatus phaseStatus;
    private boolean finished;
    private GameLifeCycleStatus lifeCycleStatus;

    private Long version;
    private Long updateCounter;
    
    private Set<PlayerResponseDTO> players = new HashSet<>();
    private Set<GamePoliticoResponseDTO> gamePoliticos = new HashSet<>();
    private Set<GameMinistryResponseDTO> gameMinistries = new HashSet<>();
    private Set<Long> readyPlayers = new HashSet<>();
    
    private ActionBlockingStatus blockingStatus;
    private ActionInstanceDTO awaitingAction;
    private List<ActionInstanceDTO> announcedActions;
    private Set<ActionType> possibleActionsByPhase = new HashSet<>();
    private Map<MinistryEnum, Set<ActionType>> authorizedMinistryAndActions = new HashMap<>();

    private TrialResponseDTO trial;

    private Long myPlayerID;
}
