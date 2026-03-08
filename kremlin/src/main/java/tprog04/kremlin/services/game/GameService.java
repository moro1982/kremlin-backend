package tprog04.kremlin.services.game;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import tprog04.kremlin.aux_classes.ActionBlockingStatus;
import tprog04.kremlin.aux_classes.GameLifeCycleStatus;
import tprog04.kremlin.aux_classes.GameStatus;
import tprog04.kremlin.aux_classes.PhaseExecutionStatus;
import tprog04.kremlin.aux_classes.SseEventType;
import tprog04.kremlin.dto.game.GameContextDTO;
import tprog04.kremlin.dto.game.GameRequestDTO;
import tprog04.kremlin.dto.game.GameResponseDTO;
import tprog04.kremlin.dto.game.GameSummaryDTO;
import tprog04.kremlin.dto.influence.assigned.AssignedResponseDTO;
import tprog04.kremlin.mapper.GameMapper;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.models.User;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.services.GameMinistryService;
import tprog04.kremlin.services.GamePoliticoService;
import tprog04.kremlin.services.PlayerService;
import tprog04.kremlin.services.influence.implementations.AssignedInfluenceService;
import tprog04.kremlin.services.notification.PlayerNotificationService;
import tprog04.kremlin.services.validation.ValidationService;

@Service
public class GameService {

    @Autowired
    private GameRepository repoGame;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private GamePoliticoService gamePolService;
    @Autowired
    private GameMinistryService gameMinService;
    @Autowired
    private AssignedInfluenceService influenceService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private PlayerNotificationService notificationService;
    @Autowired 
    private GameMapper gameMapper;
    @Autowired
    private PhaseManager phaseManager;

    public List<GameResponseDTO> getAllGames() {
        List<Game> games = this.repoGame.findAll();
        return games.stream()
                    .map( g -> {
                        return this.gameMapper.toDto(g, null);
                    })
                    .collect(Collectors.toList());
    }

    public GameResponseDTO getGameById(Long gameID) {
        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);
        return this.gameMapper.toDto(game, myPlayer.getId());
    }

    public List<GameResponseDTO> getGamesByCurrentUser() {
        
        User currentUser = this.validationService.getCurrentUser();
        List<Player> playersByUser = 
            this.playerService.getPlayersByUser(currentUser);

        List<Game> gamesByUser =
            playersByUser.stream()
                         .map(player -> this.repoGame.findByPlayer(player.getId()))
                         .collect(Collectors.toList());
        
        return gamesByUser.stream()
                          .map( g -> {
                            return this.gameMapper.toDto(g, null);
                          })
                          .collect(Collectors.toList());
    }

    public List<GameSummaryDTO> getGameSummaries() {
        User currentUser = this.validationService.getCurrentUser();
        List<Game> allGames = this.repoGame.findAll();
        return allGames.stream()
                       .map(g -> {
                           return this.gameMapper.toSummary(g, currentUser);
                       })
                       .collect(Collectors.toList());
    }

    public GameContextDTO getGameContext(Long gameID) {

        Game game = this.validationService.validateGameByID(gameID);

        GameLifeCycleStatus lifeCycleStatus = this.gameMapper.deriveGameLifeCycleStatus(game);
        if (lifeCycleStatus.equals(GameLifeCycleStatus.FINISHED)) {
            throw new IllegalStateException("Game Finished.");
        }

        return this.gameMapper.toContext(game);
    }

    public GameResponseDTO createNewGame(int maxPlayers) {
        
        if (maxPlayers < 3 || maxPlayers > 6) {
            throw new IllegalStateException(
                "Maximum number of players can't be under 3 or higher than 6."
            );
        }

        GameRequestDTO dto = new GameRequestDTO();
        dto.setCreatedAt(LocalDateTime.now());
        dto.setStartedAt(null);
        dto.setCurrentTurn(0);
        dto.setCurrentPhase(0);
        dto.setFinished(false);
        dto.setPhaseStatus(PhaseExecutionStatus.NONE);
        dto.setUpdateCounter(0L);
        dto.setBlockingStatus(null);
        dto.setMaxPlayers(maxPlayers);
        dto.setStatus(GameStatus.OPEN);

        Game newGame = this.gameMapper.toEntity(dto);
        Game saved = this.repoGame.save(newGame);
        
        return this.gameMapper.toDto(saved, null);
    }

    @Transactional
    public void lobbyToggleReady(Long gameID) {

        Game game = this.validationService.validateGameByID(gameID);
        if (game.getStartedAt() != null && 
            game.getPhaseStatus() != PhaseExecutionStatus.NONE
        ) {
            throw new IllegalStateException("Game already started.");
        }

        this.toggleReady(gameID);
    }

    @Transactional
    public void toggleReady(Long gameID) {
        User currentUser = this.validationService.getCurrentUser();
        Game game = this.validationService.validateGameByID(gameID);
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);
        Long myPlayerID = myPlayer.getId();

        if (game.getReadyPlayers().contains(myPlayerID)) {
            game.getReadyPlayers().remove(myPlayerID);
        } else {
            game.getReadyPlayers().add(myPlayerID);
        }

        this.repoGame.save(game);
    }

    @Transactional
    public void beginInfluenceAssignment(Long gameID) {

        User currentUser = this.validationService.getCurrentUser();
        Game game = this.validationService.validateGameByID(gameID);

        if (game.getStartedAt() != null &&
            game.getPhaseStatus() != PhaseExecutionStatus.NONE
        ) {
            throw new IllegalStateException("Game already started.");
        }

        this.validationService.getPlayerByUserAndGame(currentUser, game);

        if (game.getPlayers().size() != game.getMaxPlayers()) {
            throw new IllegalStateException("Game is not full.");
        }

        boolean allReady = game.getReadyPlayers().size() == game.getPlayers().size();
        if (!allReady) {
            throw new IllegalStateException("Not all Players are ready.");
        }

        // Close Lobby
        game.setStartedAt(LocalDateTime.now());
        game.setCurrentTurn(0);
        game.setCurrentPhase(null);
        game.setStatus(GameStatus.CLOSED);
        this.clearReadyPlayers(gameID);

        // Initialize GamePoliticos
        this.gamePolService.loadGamePoliticos(gameID);

        // Save game
        this.repoGame.save(game);

    }

    @Transactional
    public void confirmInfluenceAssignment(Long gameID) {
        User currentUser = this.validationService.getCurrentUser();
        Game game = this.validationService.validateGameByID(gameID);

        // Context validation
        GameLifeCycleStatus lifeCycleStatus = 
                this.gameMapper.deriveGameLifeCycleStatus(game);
        if (!lifeCycleStatus.equals(GameLifeCycleStatus.INFLUENCE_ASSIGNMENT)) {
            throw new IllegalStateException(
                "Game not in Influence Assignment phase."
            );
        }

        // Assignment complete validation
        Player myPlayer = 
                this.validationService.getPlayerByUserAndGame(currentUser, game);
        List<AssignedResponseDTO> influences =
                this.influenceService.getAssignedByPlayer(myPlayer.getId());
        this.validationService.validatePlayerAssignedInfluences(influences);

        // If reached here, mark player as ready
        game.markPlayerReady(myPlayer);

        // Save Game
        this.repoGame.save(game);

        // Notify player
        SseEventType eventType = SseEventType.GAME_UPDATE;
        String privateMessage = "Player " + myPlayer.getName() + 
                                " has completed influence assignment.\n" +
                                "Waiting for other players.\n";
        this.notificationService.broadcastGameUpdate(game, eventType.name());
        this.notificationService.notifySinglePlayer(
            game, myPlayer, SseEventType.PRIVATE_MESSAGE, privateMessage
        );

        // If all ready...
        if (game.allPlayersReady()) {
            this.finalizeInfluenceAssignment(game);
        }

    }

    private void finalizeInfluenceAssignment(Game game) {

        // Initialize Ministries
        this.gameMinService.loadGameMinistries(game.getId());
        // Assign initial ministers
        this.gameMinService.assignInitialMinisters(game.getId());

        // Start game (turn 1, phase 1)
        this.startGame(game.getId());
    }

    public GameResponseDTO startGame(Long gameID) {

        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);

        game.clearReadyPlayers();
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentTurn(1);
        game.setCurrentPhase(1);
        game.setFinished(false);
        game.setPhaseStatus(PhaseExecutionStatus.WAITING_TO_BEGIN);
        game.setBlockingStatus(ActionBlockingStatus.NONE);
        game.setCurrentAwaitingAction(null);
        game.setTrial(null);

        // ¿¿ Increment updateCounter ??
        
        Game saved = this.repoGame.save(game);

        // Notify players game has begun
        SseEventType eventType = SseEventType.GAME_START;
        String messageAll = "Game nr. " + saved.getId() + " has begun.\n" + 
                         "Phase 1 of Turn 1 is ready to begin.\n";
        
        this.notificationService.notifyAllPlayers(saved, SseEventType.GAME_MESSAGE, messageAll);
        this.notificationService.broadcastGameUpdate(saved, eventType.name());

        /* TEST ONLY */
        for (Player player : saved.getPlayers()) {
            String messageSingle = "Player " + player.getName() + 
                               " receives private message.\n";
            this.notificationService.notifySinglePlayer(
                game, player, SseEventType.PRIVATE_MESSAGE, messageSingle
            );
        }

        return this.gameMapper.toDto(saved, myPlayer.getId());
    }


    /**
     * Facade methods
     * Delegate all phase management logic to PhaseManager.
     * For controller-level usage.
    **/
    public GameResponseDTO beginCurrentPhase(Long gameID) {
        
        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);

        // Check if players ready and Phase status
        if (this.allPlayersReady(gameID) && 
            game.getPhaseStatus().equals(PhaseExecutionStatus.WAITING_TO_BEGIN)
        ) {
            // Announces phase opening
            Game saved = this.phaseManager.beginCurrentPhase(game);
            return this.gameMapper.toDto(saved, myPlayer.getId());
        }

        return this.gameMapper.toDto(game, myPlayer.getId());

    }

    public GameResponseDTO resolveAwaitingAction(Long gameID) {
        
        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);
        
        Game saved = this.phaseManager.resolveAwaitingAction(game);

        return this.gameMapper.toDto(saved, myPlayer.getId());
    }

    public GameResponseDTO confirmPhaseExecution(Long gameID) {

        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);
        
        if (!game.allPlayersReady()) {
            return this.gameMapper.toDto(game, myPlayer.getId());
        }
        
        Game saved = this.phaseManager.confirmPhaseExecution(game);
        
        return this.gameMapper.toDto( saved, myPlayer.getId() );
    }

    public GameResponseDTO endCurrentPhase(Long gameID) {

        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);

        Game saved = this.phaseManager.endCurrentPhase(game);

        return this.gameMapper.toDto(saved, myPlayer.getId());
    }

    public GameResponseDTO nextPhase(Long gameID) {

        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);

        Game saved = this.phaseManager.nextPhase(game);

        return this.gameMapper.toDto(saved, myPlayer.getId());
    }


    /**
     * Game state management methods
    **/
    @Transactional
    public GameResponseDTO markPlayerReady(Long gameID) {

        // Get Current User from Security Context
        User user = this.validationService.getCurrentUser();
        // Find Game by gameID
        Game game = this.validationService.validateGameByID(gameID);
        // Find Player by User and Game
        Player actor = this.validationService.getPlayerByUserAndGame(user, game);

        game.markPlayerReady(actor);
        Game savedGame = repoGame.save(game);
        
        // Notify
        String messageSinglePlayerReady = 
            "Player " + actor.getName() + " is ready to continue.\n";
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.GAME_MESSAGE,
            messageSinglePlayerReady
        );
        this.notificationService.broadcastGameUpdate(
            savedGame,
            SseEventType.PLAYER_READY.name()
        );

        if (game.allPlayersReady()) {

            // If all ready, notify and allow actions' resolution
            // (call confirmPhaseExecution() from frontend)

            // Notify
            String messageAllPlayersReady = "All Players ready to continue.";
            this.notificationService.notifyAllPlayers(
                savedGame, 
                SseEventType.GAME_MESSAGE,
                messageAllPlayersReady
            );
            this.notificationService.broadcastGameUpdate(
                savedGame,
                SseEventType.ALL_PLAYERS_READY.name()
            );
        }

        return this.gameMapper.toDto(savedGame, actor.getId());
    }

    public GameResponseDTO getGameState(Long gameID) {
        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);
        return this.gameMapper.toDto(game, myPlayer.getId());
    }
    
    public void clearReadyPlayers(Long game_id) {
        Game game = this.validationService.validateGameByID(game_id);
        game.clearReadyPlayers();
    }

    public boolean allPlayersReady(Long game_id) {
        Game game = this.validationService.validateGameByID(game_id);
        return game.allPlayersReady();
    }

    public Long getUpdateCounter(Long game_id) {
        Game gameStatus = this.validationService.validateGameByID(game_id);
        return gameStatus.getUpdateCounter();
    }


    /**
     * Trial management (facade methods)
     * Delegate all trial logic to TrialService (called by PhaseManager)
    **/
    public GameResponseDTO beginTrial(Long gameID) {
        Game savedGame = this.phaseManager.beginTrial(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, savedGame);
        return this.gameMapper.toDto(savedGame, myPlayer.getId());
    }

    public GameResponseDTO readyAfterTrialVote(Long gameID) {
        Game savedGame = this.phaseManager.readyAfterTrialVote(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, savedGame);
        return this.gameMapper.toDto(savedGame, myPlayer.getId());
    }

    public GameResponseDTO resumeTrialVoting(Long gameID) {
        Game savedGame = this.phaseManager.resumeTrialVoting(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, savedGame);
        return this.gameMapper.toDto(savedGame, myPlayer.getId());
    }

}
