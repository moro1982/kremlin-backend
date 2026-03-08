package tprog04.kremlin.mapper;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tprog04.kremlin.aux_classes.GameLifeCycleStatus;
import tprog04.kremlin.aux_classes.GameStatus;
import tprog04.kremlin.aux_classes.PhaseType;
import tprog04.kremlin.dto.game.GameContextDTO;
import tprog04.kremlin.dto.game.GameRequestDTO;
import tprog04.kremlin.dto.game.GameResponseDTO;
import tprog04.kremlin.dto.game.GameSummaryDTO;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.User;
import tprog04.kremlin.repositories.GamePoliticoRepository;
import tprog04.kremlin.repositories.PlayerRepository;
import tprog04.kremlin.services.GameMinistryService;
import tprog04.kremlin.services.game.ActionService;

@Component
public class GameMapper {

    @Autowired
    private PlayerRepository repoPlayer;
    @Autowired
    private GamePoliticoRepository repoGamePol;
    @Autowired
    private PlayerMapper playerMapper;
    @Autowired
    private GamePoliticoMapper gamePolMapper;
    @Autowired
    private GameMinistryMapper gameMinMapper;
    @Autowired
    private ActionMapper actionMapper;
    @Autowired
    private TrialMapper trialMapper;
    @Autowired
    private GameMinistryService gameMinService;
    @Autowired
    private ActionService actionService;
    
    public GameResponseDTO toDto(Game entity, Long myPlayerID) {
        GameResponseDTO dto = new GameResponseDTO();

        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCurrentTurn(entity.getCurrentTurn());
        Integer entityPhase = entity.getCurrentPhase();
        if (entityPhase != null) {
            dto.setCurrentPhase(PhaseType.fromOrder(entity.getCurrentPhase()));
        } else {
            dto.setCurrentPhase(null);
        }
        dto.setPhaseStatus(entity.getPhaseStatus());
        dto.setFinished(entity.isFinished());
        dto.setLifeCycleStatus(this.deriveGameLifeCycleStatus(entity));

        dto.setVersion(entity.getVersion());
        dto.setUpdateCounter(entity.getUpdateCounter());
        
        dto.setPlayers(
            entity.getPlayers().stream()
                               .map( p -> {
                                return this.playerMapper.toDto(p, myPlayerID);
                               } )
                               .collect(Collectors.toSet())
        );
        dto.setGamePoliticos(
            entity.getGamePoliticos().stream()
                                     .map(gamePolMapper::toDto)
                                     .collect(Collectors.toSet())
        );
        dto.setGameMinistries(
            this.gameMinService.getGameMinistriesByGame(entity.getId())
                               .stream()
                               .map(gameMinMapper::toDto)
                               .collect(Collectors.toSet())
        );
        dto.setReadyPlayers(entity.getReadyPlayers());

        if (entity.getBlockingStatus() != null) {
            dto.setBlockingStatus(entity.getBlockingStatus());
        }
        if (entity.getCurrentAwaitingAction() != null) {
            dto.setAwaitingAction(
                this.actionMapper.toDTO(entity.getCurrentAwaitingAction())
            );
        }
        dto.setAnnouncedActions(this.actionService.getPendingActions(entity.getId()));
        dto.setPossibleActionsByPhase(
            this.actionService.getPossibleActionsByPhase(
                PhaseType.fromOrder(entity.getCurrentPhase())
            )
        );

        if (entity.getTrial() != null) {
            dto.setTrial(this.trialMapper.toDto(entity.getTrial()));
        }
        dto.setMyPlayerID(myPlayerID);

        return dto;
    }

    public Game toEntity(GameRequestDTO dto) {
        Game entity = new Game();

        entity.setCreatedAt(dto.getCreatedAt());
        entity.setStartedAt(dto.getStartedAt());
        entity.setCurrentTurn(dto.getCurrentTurn());
        entity.setCurrentPhase(dto.getCurrentPhase());
        entity.setFinished(dto.isFinished());
        entity.setUpdateCounter(dto.getUpdateCounter());
        entity.setBlockingStatus(dto.getBlockingStatus());
        entity.setMaxPlayers(dto.getMaxPlayers());
        entity.setStatus(dto.getStatus());
        entity.setPlayers(
            this.repoPlayer.findAllById(dto.getPlayers())
                           .stream()
                           .collect(Collectors.toSet())
        );
        entity.setGamePoliticos(
            this.repoGamePol.findAllById(dto.getGamePoliticos())
                            .stream()
                            .collect(Collectors.toSet())
        );
        entity.setReadyPlayers(dto.getReadyPlayers());

        return entity;
    }

    public GameSummaryDTO toSummary(Game game, User user) {

        Long id = game.getId();

        boolean iAmParticipant = game.hasPlayer(user);

        int playerCount = game.getPlayers().size();
        int maxPlayers = game.getMaxPlayers();

        GameLifeCycleStatus lifeCycleStatus = this.deriveGameLifeCycleStatus(game);
        GameStatus status = game.getStatus();

        boolean joinable = 
            lifeCycleStatus == GameLifeCycleStatus.LOBBY
            && status.equals(GameStatus.OPEN)
            && !iAmParticipant
            && playerCount < maxPlayers;

        boolean resumable = 
            iAmParticipant
            && lifeCycleStatus != GameLifeCycleStatus.FINISHED;

        LocalDateTime createdAt = game.getCreatedAt();
        LocalDateTime startedAt = game.getStartedAt();

        GameSummaryDTO dto = new GameSummaryDTO(
            id, 
            lifeCycleStatus, 
            playerCount, 
            maxPlayers, 
            iAmParticipant, 
            joinable, 
            resumable, 
            createdAt,
            startedAt
        );

        return dto;
    }

    public GameLifeCycleStatus deriveGameLifeCycleStatus(Game game) {

        if (game.isFinished()) {
            return GameLifeCycleStatus.FINISHED;
        }

        if (game.getStartedAt() == null) {
            return GameLifeCycleStatus.LOBBY;
        }

        if (game.getCurrentTurn() == 0) {
            return GameLifeCycleStatus.INFLUENCE_ASSIGNMENT;
        }

        return GameLifeCycleStatus.RUNNING;
    }

    public GameContextDTO toContext(Game game) {

        GameLifeCycleStatus lifeCycleStatus = this.deriveGameLifeCycleStatus(game);

        GameContextDTO dto = new GameContextDTO();
        dto.setGameID(game.getId());
        dto.setLifeCycleStatus(lifeCycleStatus);
        dto.setStatus(game.getStatus());
        dto.setMaxPlayers(game.getMaxPlayers());
        dto.setPlayers(
            game.getPlayers().stream()
                             .map(playerMapper::toLobbyPlayerDTO)
                             .collect(Collectors.toList())
        );

        return dto;
    }
}
