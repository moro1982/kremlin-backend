package tprog04.kremlin.services.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tprog04.kremlin.aux_classes.ActionBlockingStatus;
import tprog04.kremlin.aux_classes.PhaseExecutionStatus;
import tprog04.kremlin.aux_classes.PhaseType;
import tprog04.kremlin.aux_classes.SseEventType;
import tprog04.kremlin.models.ActionInstance;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.services.GamePoliticoService;
import tprog04.kremlin.services.game.trial.TrialService;
import tprog04.kremlin.services.notification.PlayerNotificationService;
import tprog04.kremlin.services.validation.ValidationService;

@Service
public class PhaseManager {

    @Autowired
    private GameRepository repoGame;
    @Autowired
    private ActionService actionService;
    @Autowired
    private GamePoliticoService gamePolService;
    @Autowired
    private TrialService trialService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private PlayerNotificationService notificationService;

    public Game beginCurrentPhase( Game game ) {

        if (game.getPhaseStatus() != PhaseExecutionStatus.WAITING_TO_BEGIN) {
            return game;
        }

        // Clean ready players
        game.clearReadyPlayers();
        
        // Automatic events or actions ocurred at the beginning of phase
        this.executeBeginPhaseEvents(game);

        /* Phase flow: */
        /*  * Open interval for action announcements.
            * Notify Players to declare any action among those allowed in this phase
                (PlayerNotificationService).
            * Flow waits for Players' action announcements.
        */
        // Targeted actions only allowed in certain phases.

        // Change phase status
        game.setPhaseStatus(PhaseExecutionStatus.OPEN_FOR_ACTIONS);

        // Save Game state
        Game saved = this.repoGame.save(game);

        // Notification
        SseEventType eventType = SseEventType.PHASE_START;        
        this.notificationService.broadcastGameUpdate(saved, eventType.name());
        PhaseType phase = PhaseType.fromOrder(saved.getCurrentPhase());
        this.notificationService.notifyAllPlayers(
            game, 
            eventType, 
            Map.of(
                "phase", phase.name(),
                "turn", saved.getCurrentTurn()
            )
        );
        String message = "Phase " + PhaseType.fromOrder(saved.getCurrentPhase()) + 
                         " has begun.\n" + "Declare Player actions for this phase.\n";
        this.notificationService.notifyAllPlayers(
            saved, 
            SseEventType.GAME_MESSAGE, 
            message
        );

        return saved;
    }

    @Transactional
    public Game nextPhase( Game game ) {

        Game savedGame = new Game();
        
        if (this.isLastPhase(game)) {    
            savedGame = this.nextTurn(game);
        } else {
            savedGame = this.advancePhase(game);
            SseEventType eventType = SseEventType.ADVANCE_PHASE;
            String message = "Phase " + savedGame.getCurrentPhase() + " in Turn " + 
                             savedGame.getCurrentTurn() + " is ready to begin.\n";
            this.notificationService.notifyAllPlayers( savedGame, eventType, message );
            this.notificationService.broadcastGameUpdate(savedGame, eventType.name());
        }
        return savedGame;
    }

    // Called from nextPhase()
    private boolean isLastPhase( Game game ) {
        return game.getCurrentPhase() >= PhaseType.values().length;
    }

    // Called from nextPhase()
    @Transactional
    private Game nextTurn( Game game ) {

        Game saved = new Game();

        if (game.getCurrentTurn() >= 10) {
            
            game.setFinished(true);
            saved = this.repoGame.save(game);
            
            SseEventType eventType = SseEventType.GAME_END;
            String message = "Current game has come to an end.\n";
            this.notificationService.notifyAllPlayers(saved, eventType, message);
            this.notificationService.broadcastGameUpdate(saved, eventType.name());

        } else {

            game.setCurrentTurn(game.getCurrentTurn() + 1);
            game.setCurrentPhase(1);
            game.setPhaseStatus(PhaseExecutionStatus.WAITING_TO_BEGIN);
            game.clearReadyPlayers();
            saved = this.repoGame.save(game);
    
            SseEventType eventType = SseEventType.ADVANCE_TURN;
            String message = "Turn " + saved.getCurrentTurn() + " begins." + 
                             " Waiting for beginning of Phase 1.\n";
            this.notificationService.notifyAllPlayers(saved, eventType, message);
            this.notificationService.broadcastGameUpdate(saved, eventType.name());
        }

        return saved;
    }

    // Called from nextPhase()
    @Transactional
    private Game advancePhase( Game game ) {
        game.setCurrentPhase(game.getCurrentPhase() + 1);
        game.setPhaseStatus(PhaseExecutionStatus.WAITING_TO_BEGIN);
        game.clearReadyPlayers();
        Game saved = this.repoGame.save(game);
        return saved;
    }

    @Transactional
    public Game endCurrentPhase( Game game ) {
        
        game.clearReadyPlayers();
        game.setPhaseStatus(PhaseExecutionStatus.FINISHED);
        Game saved = this.repoGame.save(game);

        // Notify
        SseEventType eventType = SseEventType.PHASE_END;
        PhaseType phase = PhaseType.fromOrder(saved.getCurrentPhase());
        String message = "Phase nr. " + phase.name() + " has ended.\n" + 
                         "No more actions allowed this turn.\n";

        this.notificationService.notifyAllPlayers(
            saved, 
            eventType, 
            Map.of(
                "phase", phase.name(),
                "turn", saved.getCurrentTurn()
            )
        );
        this.notificationService.notifyAllPlayers(
            saved,
            SseEventType.GAME_MESSAGE,
            message
        );
        this.notificationService.broadcastGameUpdate(game, eventType.name());
        
        return saved;
    }

    @Transactional
    public Game resolveAwaitingAction( Game game ) {
        
        ActionInstance awaitingAction = game.getCurrentAwaitingAction();

        // Resolve awaiting action
        Game savedGame  = this.actionService.resolveAwaitingAction(game);

        // If purge has failed, end current phase and advance to next phase
        if (savedGame.getBlockingStatus().equals(ActionBlockingStatus.FAILED_PURGE_BLOCK)) {
            // Clean awaiting action status
            game.setBlockingStatus(ActionBlockingStatus.NONE);
            game.setCurrentAwaitingAction(null);
            // End current phase and advance to next phase
            savedGame = this.endCurrentPhase(game);
            savedGame = this.nextPhase(game);
            // Save game state
            savedGame = this.repoGame.save(savedGame);

            // Notifications for failed purge
            this.notificationService.notifyAllPlayers(
                savedGame, 
                SseEventType.AWAITING_ACTION_FAILED, 
                Map.of(
                    "actingPoliticoID", awaitingAction.getActingGamePolitico().getId(),
                    "targetPoliticoID", awaitingAction.getTargetGamePolitico().getId()
                )
            );
            // Forced phase end
            this.notificationService.notifyAllPlayers(
                savedGame, 
                SseEventType.FORCED_PHASE_END, 
                Map.of(
                    "phase", PhaseType.fromOrder(savedGame.getCurrentPhase()),
                    "reason", SseEventType.AWAITING_ACTION_FAILED.name()
                )
            );
            // Game update
            this.notificationService.broadcastGameUpdate(
                savedGame,
                SseEventType.AWAITING_ACTION_FAILED.name()
            );
            // Early return
            return savedGame;
        }

        // Clean awaiting action status and save Game state
        game.setBlockingStatus(ActionBlockingStatus.NONE);
        game.setCurrentAwaitingAction(null);
        savedGame = this.repoGame.save(savedGame);
        
        // Notify
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.AWAITING_ACTION_RESOLVED,
            Map.of(
                "actionType", awaitingAction.getType().name(),
                "result", awaitingAction.getStatus().name()
            )
        );
        // Game update
        this.notificationService.broadcastGameUpdate(
            savedGame,
            SseEventType.AWAITING_ACTION_RESOLVED.name()
        );

        return savedGame;
    }

    @Transactional
    public Game confirmPhaseExecution( Game game ) {

        // Change Phase status
        game.setPhaseStatus(PhaseExecutionStatus.RESOLVING_ACTIONS);
        Game savedGame = this.repoGame.save(game);

        // Notify game update
        this.notificationService.broadcastGameUpdate(
            savedGame, 
            SseEventType.PHASE_EVENT.name()
        );

        // Notify phase under resolution
        String pendingActionsMessage = 
            "Resolving pending actions and end-of-phase events in this phase.\n";
        this.notificationService.notifyAllPlayers(
            game, 
            SseEventType.GAME_MESSAGE, 
            pendingActionsMessage
        );

        // Resolve pending actions in this phase
        this.actionService.resolvePendingActions( game.getId() );
        
        // Execute end phase events
        this.executeEndPhaseEvents(game);
        
        // Clean ready Players for next phase, set PhaseStatus, and save game state
        game.clearReadyPlayers();
        savedGame = this.repoGame.save(game);

        // Notify phase execution
        this.notificationService.broadcastGameUpdate(
            savedGame, 
            SseEventType.PHASE_EXECUTED.name()
        );

        // Advance to next phase
        savedGame = this.nextPhase(game);

        return savedGame;
    }

    /* Phase events */
    public void executeBeginPhaseEvents( Game game ) {

        PhaseType phase = PhaseType.fromOrder(game.getCurrentPhase());

        switch (phase) {
            case CURES:
                System.out.println("Executing Cures begin phase events.\n");
                break;
            case PURGE:
                System.out.println("Executing Purge phase.\n");
                break;
            case SPY_INVESTIGATION:
                System.out.println("Executing Spy Investigation begin phase events.\n");
                this.execute_SPY_INVESTIGATION_Events(game);
                break;
            case HEALTH:
                System.out.println("Executing Health phase.\n");
                break;
            case FUNERAL_COMMISSION:
                System.out.println("Executing Funeral Commission phase.\n");
                break;
            case REPLACEMENT:
                System.out.println("Executing Replacement phase.\n");
                break;
            case REHABILITATION:
                System.out.println("Executing Rehabilitation phase.\n");
                break;
            case PARADE:
                System.out.println("Executing Parade phase.\n");
                break;
            default:
                break;
        }
    }

    public void executeEndPhaseEvents( Game game ) {
        PhaseType phase = PhaseType.fromOrder(game.getCurrentPhase());

        switch (phase) {
            case CURES:
                System.out.println("Executing Cures end phase events.\n");
                this.execute_CURES_Events(game);
                break;
            case PURGE:
                System.out.println("Executing Purge phase.\n");
                break;
            case SPY_INVESTIGATION:
                System.out.println("Executing Spy Investigation end phase events.\n");
                break;
            case HEALTH:
                System.out.println("Executing Health phase.\n");
                break;
            case FUNERAL_COMMISSION:
                System.out.println("Executing Funeral Commission phase.\n");
                break;
            case REPLACEMENT:
                System.out.println("Executing Replacement phase.\n");
                break;
            case REHABILITATION:
                System.out.println("Executing Rehabilitation phase.\n");
                break;
            case PARADE:
                System.out.println("Executing Parade phase.\n");
                break;
            default:
                break;
        }
    }

    
    /* Each phase events execution */
    @Transactional
    private void execute_CURES_Events( Game game ) {
        // Filter GamePoliticos (only first 8 Ministers)
        List<GamePolitico> politburoMembers = this.gamePolService.getPolitburoMembers(game);
        // Get GamePoliticos in Siberia
        List<GamePolitico> inSiberia = this.gamePolService.getPoliticosInSiberia(game);
        // Final list of GamePoliticos who will age -> politburo members + politicos in Siberia
        List<GamePolitico> ageAffectedPoliticos = politburoMembers;
        ageAffectedPoliticos.addAll(inSiberia);

        /* Aging process */
        for (GamePolitico gamePolitico : ageAffectedPoliticos) {
            this.gamePolService.ageGamePolitico(gamePolitico);
            this.gamePolService.saveGamePolitico(gamePolitico);
        }

        // Notify
        Map<Long, Integer> affectedPoliticosMap = new HashMap<>();
        ageAffectedPoliticos.stream()
                            .map( gp -> 
                                 affectedPoliticosMap.put(gp.getId(), gp.getCurrentAge())
                            );
        this.notificationService.notifyAllPlayers(
            game, 
            SseEventType.PHASE_EVENT, 
            affectedPoliticosMap
        );
    }

    @Transactional
    private void execute_SPY_INVESTIGATION_Events( Game game ) {
        
        int currentTurn = game.getCurrentTurn();
        Set<GamePolitico> gamePoliticos = game.getGamePoliticos();
        gamePoliticos
                .stream()
                .map(gamePol -> {    
                    /* CHECK -> If clean GamePolitico is still immune to investigations */
                    if (gamePol.getImmuneToInvestigationsUntilTurn() != null &&
                        gamePol.getImmuneToInvestigationsUntilTurn() <= currentTurn
                    ) {
                        gamePol.setImmuneToInvestigationsUntilTurn(
                            null
                        );
                    }
                    
                    // Update investigation count at beginning of phase 3
                    gamePol.setInvestigationCountAtPhaseStart(
                        gamePol.getInvestigationCount()
                    );
                    GamePolitico saved = this.gamePolService.saveGamePolitico(gamePol);
                    return saved;
                })
                .collect(Collectors.toSet());
        // Notify
        this.notificationService.notifyAllPlayers(
            game, 
            SseEventType.PHASE_EVENT,
            "Investigations at phase start updated."
        );
    }


    /* Trial */
    @Transactional
    public Game beginTrial( Long gameID ) {
        // Find and validate Game
        Game game = this.validationService.validateGameByID(gameID);
        // Begin trial
        Game savedGame = this.trialService.beginTrial(game);
        return savedGame;
    }

    @Transactional
    public Game readyAfterTrialVote( Long gameID ) {
        
        Game game = this.validationService.validateGameByID(gameID);

        this.trialService.validateTrialAwaitingVotes(game);
        
        Player player = this.validationService.getPlayerByUserAndGame(
            this.validationService.getCurrentUser(), game
        );
        
        // Validate if Player has voted with ALL controlled voters
        this.trialService.validatePlayerHasVotedAll(game, player);
        
        // Mark player as ready and save game state
        game.getReadyPlayers().add(player.getId());
        Game savedGame = this.repoGame.save(game);

        // Notify player ready
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.PLAYER_READY,
            Map.of( "playerID", player.getId() )
        );

        // All set??
        if (this.trialService.allVotersReady(game)) {
            
            /* When all voters ready, activate Resume Trial button in frontend */
            int totalVotes = game.getTrial().getVotes().size();
            this.notificationService.notifyAllPlayers(
                savedGame, 
                SseEventType.TRIAL_END_VOTING, 
                Map.of( "totalVotes", totalVotes )
            );
            
            /* LEGACY */
            // return this.resumeTrialVoting(game);
        }

        return savedGame;
    }

    @Transactional
    public Game resumeTrialVoting( Long gameID ) {

        // Validate Game ID
        Game game = this.validationService.validateGameByID(gameID);
        // Check trial state
        this.trialService.validateTrialAwaitingVotes(game);
        // Check if all eligible Politicos have cast their vote
        if (!trialService.allVotesCast(game)) {
            throw new IllegalStateException(
                "Not all eligible votes have been cast.\n"
            );
        }

        // Resolve Trial
        Game savedGame = this.trialService.resolveTrial(game);
        
        return savedGame;
    }

}
