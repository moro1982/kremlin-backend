package tprog04.kremlin.services.game;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import tprog04.kremlin.aux_classes.ActionBlockingStatus;
import tprog04.kremlin.aux_classes.ActionStatus;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;
import tprog04.kremlin.aux_classes.PhaseType;
import tprog04.kremlin.aux_classes.SseEventType;
import tprog04.kremlin.aux_classes.TrialStatus;
import tprog04.kremlin.dto.actionInstance.ActionInstanceDTO;
import tprog04.kremlin.dto.influence.declared.DeclaredRequestDTO;
import tprog04.kremlin.mapper.ActionMapper;
import tprog04.kremlin.models.ActionInstance;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GameMinistry;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.InfluenceAssigned;
import tprog04.kremlin.models.InfluenceDeclared;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.models.Trial;
import tprog04.kremlin.models.TrialVote;
import tprog04.kremlin.models.User;
import tprog04.kremlin.repositories.ActionInstanceRepository;
import tprog04.kremlin.repositories.GamePoliticoRepository;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.PlayerRepository;
import tprog04.kremlin.repositories.TrialRepository;
import tprog04.kremlin.repositories.TrialVoteRepository;
import tprog04.kremlin.services.ControlPoliticoService;
import tprog04.kremlin.services.GameMinistryService;
import tprog04.kremlin.services.GamePoliticoService;
import tprog04.kremlin.services.game.dice.implementations.RandomDiceService;
import tprog04.kremlin.services.game.trial.TrialService;
import tprog04.kremlin.services.influence.implementations.DeclaredInfluenceService;
import tprog04.kremlin.services.notification.PlayerNotificationService;
import tprog04.kremlin.services.validation.ActionAuthorizationService;
import tprog04.kremlin.services.validation.ValidationService;

@Service
public class ActionService {

    @Autowired
    private GameRepository repoGame;
    @Autowired
    private PlayerRepository repoPlayer;
    @Autowired
    private ActionInstanceRepository repoAction;
    @Autowired
    private GamePoliticoRepository repoGamePolitico;
    @Autowired
    private TrialRepository repoTrial;
    @Autowired
    private TrialVoteRepository repoTrialVote;
    @Autowired
    private ActionMapper actionMapper;
    @Autowired
    private GamePoliticoService gamePolService;
    @Autowired
    private GameMinistryService gameMinService;
    @Autowired
    private DeclaredInfluenceService declareService;
    @Autowired
    private PlayerNotificationService notificationService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private ActionAuthorizationService actionAuthorizationService;
    @Autowired
    private ControlPoliticoService controlService;
    @Autowired
    private RandomDiceService diceService;
    @Autowired
    private TrialService trialService;
    
    /* Announce action */
    @Transactional
    public ActionInstanceDTO announceAction(ActionInstanceDTO dto) {

        // Get current User from Security Context
        User user = this.validationService.getCurrentUser();
        // Get Game by DTO's game ID
        Game game = this.validationService.validateGameByID(dto.getGameID());
        // Get valid Player for this User and Game
        Player actor = this.validationService.getPlayerByUserAndGame(user, game);
        
        // Validate Action in Phase
        this.validationService.validateActionAllowedInPhase(dto.getType(), game);

        // Create ActionInstance from DTO + valid Player + state data.
        ActionInstance action = this.actionMapper.toEntity(dto);
        action.setActor(actor);
        action.setStatus(ActionStatus.ANNOUNCED);
        action.setCreatedAt(LocalDateTime.now());
        action.setPriority(dto.getType().getPriority());
        action.setTurn(game.getCurrentTurn());
        action.setPhase(game.getCurrentPhase());

        // Validate & authorize action
        this.actionAuthorizationService.authorizeAnnounce(action.getType(), actor, game, action);

        // Save ActionInstance
        ActionInstance savedAction = this.repoAction.save(action);
        
        String message;
        String actingPoliticoName;
        String actingMinistryName;
        String targetPoliticoName;
        String controllingPlayerName;
        Trial trial = new Trial();
        Game savedGame;

        /* ANNOUNCEMENT EFFECTS FOR CERTAIN TYPES OF ACTION */
        switch (savedAction.getType()) {
            case DECLARE_INFLUENCE:
                // if DECLARE_INFLUENCE => execute immediately and return
                this.executeAction(savedAction);
                return this.actionMapper.toDTO(savedAction);
            case PURGE_ATTEMPT:
                // if PURGE_ATTEMPT, announce and wait for responses.
                game.setBlockingStatus(ActionBlockingStatus.AWAITING_PURGE_RESPONSE);
                game.setCurrentAwaitingAction(savedAction);
                savedGame = this.repoGame.save(game);
                actingPoliticoName = savedAction.getActingGamePolitico().getPolitico()
                                                                        .getName();
                actingMinistryName = savedAction.getActingGamePolitico().getGameMinistry()
                                                .getMinistry().getName().toString();
                targetPoliticoName = savedAction.getTargetGamePolitico().getPolitico()
                                                                        .getName();
                controllingPlayerName = action.getActor().getName();
                message = "Acting minister " + actingPoliticoName + " in Ministry " +
                         actingMinistryName + "and controlled by " + controllingPlayerName + 
                         " announces the purge of " + targetPoliticoName +
                         ".\nInfluence declarations or Exile allowed.\n";
                // Notify.
                this.notificationService.notifyAllPlayers(
                    savedGame, 
                    SseEventType.GAME_MESSAGE,
                    message
                );
                this.notificationService.notifyAllPlayers(
                    savedGame, 
                    SseEventType.AWAITING_ACTION_ANNOUNCED,
                    Map.of(
                        "actionType", savedAction.getType().name(),
                        "actingPoliticoID", savedAction.getActingGamePolitico().getId(),
                        "targetPoliticoID", savedAction.getTargetGamePolitico().getId()
                    )
                );
                break;
            case EXILE_ESCAPE:
                /* EXILE_ESCAPE -> execute immediately and return */
                // (Effects: 
                //   * cancel current Purge or Trial
                //   * send target into Exile
                this.executeAction(savedAction);
                return this.actionMapper.toDTO(savedAction);
            case EXILE_RETURN:
                /* EXILE_RETURN -> execute immediately and return */
                // Effects: 
                //  * return target from Exile to PEOPLE
                //  * discard half of highest declared influence
                this.executeAction(savedAction);
                return this.actionMapper.toDTO(savedAction);
            case OPEN_TRIAL:
                /* if OPEN_TRIAL, wait for responses */
                // Set new Trial and save
                trial.setGame(game);
                trial.setAccused(savedAction.getTargetGamePolitico());
                trial.setProsecutor(savedAction.getActingGamePolitico());
                trial.setStatus(TrialStatus.AWAITING_RESPONSE);
                trial.setTurn(game.getCurrentTurn());
                trial.setInvestigationCountAtStart(
                    action.getTargetGamePolitico().getInvestigationCountAtPhaseStart()
                );
                this.repoTrial.save(trial);
                // Attach Trial to saved Action
                savedAction.setTrial(trial);
                savedAction = this.repoAction.save(savedAction);
                // Set blocking/awating Action in Game and save
                game.setTrial(trial);
                game.setBlockingStatus(ActionBlockingStatus.AWAITING_TRIAL_RESPONSE);
                game.setCurrentAwaitingAction(savedAction);
                savedGame = this.repoGame.save(game);

                actingPoliticoName = savedAction.getActingGamePolitico().getPolitico().getName();
                actingMinistryName = savedAction.getActingGamePolitico().getGameMinistry()
                                           .getMinistry().getName().toString();
                targetPoliticoName = savedAction.getTargetGamePolitico().getPolitico().getName();
                controllingPlayerName = savedAction.getActor().getName();
                message = "Acting minister " + actingPoliticoName + " in Ministry " +
                         actingMinistryName + " controlled by " + controllingPlayerName + 
                         " announces the opening of Trial against " + targetPoliticoName +
                         ".\nInfluence declarations or Exile allowed.\n";
                // Notify.
                this.notificationService.notifyAllPlayers(
                    savedGame, 
                    SseEventType.GAME_MESSAGE,
                    message
                );
                this.notificationService.notifyAllPlayers(
                    savedGame, 
                    SseEventType.AWAITING_ACTION_ANNOUNCED,
                    Map.of(
                        "actionType", savedAction.getType().name(),
                        "actingPoliticoID", savedAction.getActingGamePolitico().getId(),
                        "targetPoliticoID", savedAction.getTargetGamePolitico().getId()
                    )
                );
                this.notificationService.broadcastGameUpdate(
                    savedGame,
                    SseEventType.AWAITING_ACTION_ANNOUNCED.name()
                );
                break;
            case CAST_TRIAL_VOTE:
                /*  
                 * if CAST_TRIAL_VOTE, create vote and save in Trial
                 * Cancellations due to change of voter's control are allowed
                */

                // Check if current valid Trial for this Game
                trial = game.getCurrentAwaitingAction().getTrial();
                if (trial == null) {
                    throw new IllegalStateException(
                        "No active Trial to cast a vote.\n"
                    );
                }
                
                // Create TrialVote
                TrialVote vote = new TrialVote();
                vote.setTrial(trial);
                vote.setVoter(savedAction.getActingGamePolitico());
                vote.setVote(savedAction.getTrialVoteValue());
                vote.setTurn(game.getCurrentTurn());
                vote.setPhase(game.getCurrentPhase());
                // Save TrialVote
                this.repoTrialVote.save(vote);
                // Add vote to Trial's vote list, and save Trial
                trial.getVotes().add(vote);
                this.repoTrial.save(trial);
                // Mark vote action as RESOLVED, and save
                savedAction.setStatus(ActionStatus.RESOLVED);
                savedAction.setResolved(true);
                savedAction = this.repoAction.save(savedAction);
                // Save game state
                savedGame = this.repoGame.save(game);

                /* Notifications */
                // Notify all (message)
                message = "Politico " + vote.getVoter().getPolitico().getName() + 
                         " has cast his/her vote on trial against " + 
                         vote.getTrial().getAccused().getPolitico().getName() + 
                         ".\nCancellations due to change of voter's control are allowed.\n" +
                         "If already voted, mark ready to await trial's resolution.\n";
                this.notificationService.notifyAllPlayers(
                    savedGame, 
                    SseEventType.GAME_MESSAGE, 
                    message
                );
                // Private vote confirmation (received by voter's controller only)
                this.notificationService.notifySinglePlayer(
                    savedGame,
                    actor, 
                    SseEventType.PRIVATE_MESSAGE,
                    Map.of(
                        "voterPoliticoID", vote.getVoter().getPolitico().getId(),
                        "voteValue", vote.getVote().name()
                    )
                );
                // Game update
                this.notificationService.broadcastGameUpdate(
                    savedGame, 
                    SseEventType.ACTION_EXECUTED.name()
                );
                
                break;
            case CONDEMNATION:
                /* After announced, await possible response (NEGATE_CONDEMNATION) */
                // It becomes an awaiting action
                game.setBlockingStatus(ActionBlockingStatus.AWAITING_CONDEMNATION_RESPONSE);
                game.setCurrentAwaitingAction(savedAction);
                savedGame = this.repoGame.save(game);
                // Notify
                actingPoliticoName = savedAction.getActingGamePolitico().getPolitico()
                                                                        .getName();
                actingMinistryName = savedAction.getActingGamePolitico().getGameMinistry()
                                                .getMinistry().getName().toString();
                targetPoliticoName = savedAction.getTargetGamePolitico().getPolitico()
                                                                        .getName();
                controllingPlayerName = action.getActor().getName();
                message = "Target Candidate " + targetPoliticoName +
                         " is being pointed for a direct Condemnation to Siberia.\n" + 
                         ".\nInfluence declarations or Negate Condemnation are allowed.\n";
                // Notify.
                this.notificationService.notifyAllPlayers(
                    savedGame, 
                    SseEventType.GAME_MESSAGE,
                    message
                );
                this.notificationService.notifyAllPlayers(
                    savedGame, 
                    SseEventType.AWAITING_ACTION_ANNOUNCED,
                    Map.of(
                        "actionType", savedAction.getType().name(),
                        "actingPoliticoID", savedAction.getActingGamePolitico().getId(),
                        "targetPoliticoID", savedAction.getTargetGamePolitico().getId()
                    )
                );
                break;
            case NEGATE_CONDEMNATION:
                /* After announced, execute immediately */
                // Effects:
                //  * Cancel an awaiting Condemnation
                //  * Ages acting Minister +5 years
                this.executeAction(savedAction);
                return this.actionMapper.toDTO(savedAction);
            default:
                // Don't execute action yet (pendant action until confirm or cancel).
                message = "Player " + savedAction.getActor().getName() + 
                         " announces " + savedAction.getType().toString() +
                         " action.\n";
                break;
        }

        return this.actionMapper.toDTO(savedAction);
    }

    /* Cancel action */
    @Transactional
    public ActionInstanceDTO cancelAction(ActionInstanceDTO dto) {

        // Get current User from Security Context
        User user = this.validationService.getCurrentUser();
        // Get Game by DTO's game ID
        Game game = this.validationService.validateGameByID(dto.getGameID());
        // Get valid Player for this User and Game
        Player actor = this.validationService.getPlayerByUserAndGame(user, game);

        // Find ActionInstance by ID
        ActionInstance action = this.repoAction.findById(dto.getId())
                                               .orElseThrow(
                                                  () -> new RuntimeException(
                                                    "Action Not Found.\n"
                                                  )
                                               );
        
        // Validate if ActionInstance was announced by Player
        // this.validationService.validateActionOwner(action, actor);
        /* ( WRONG!! Must control Politico involved in action (acting, target, etc.) */
        
        // Validate if cancelling Player controls Politico involved in action
        this.validationService.validatePlayerControlsPoliticoInAction(actor, action);
        
        // Check if it's ANNOUNCED before cancelling
        this.validationService.validateActionIsAnnounced(action);

        // If reach here, proceed with cancel
        action.setStatus(ActionStatus.CANCELLED);
        this.repoAction.save(action);
        this.repoGame.save(game);

        // Notify
        SseEventType eventType = SseEventType.ACTION_CANCELLED;
        String message = "Player " + action.getActor().getName() + 
                         " cancels action " + action.getType().toString() + ".\n";
        this.notificationService.notifyAllPlayers(
            game,
            eventType,
            message
        );
        this.notificationService.broadcastGameUpdate(game, eventType.name());

        return this.actionMapper.toDTO(action);
    }

    /* Execute single action */
    public void executeAction(ActionInstance action) {

        /* Evaluate if @Transactional as well, although it's now called from other
            @Transactional methods.
        */

        // Validate if action is still pending (or has been interrupted somehow)
        if (action.getStatus() != ActionStatus.ANNOUNCED) {
            // Just skip it, 'cause an exception would break entire resolution chain.
            return;
        }

        SseEventType eventType;
        String message;

        try {
            // Different resolution for each action type
            switch (action.getType()) {
                case DECLARE_INFLUENCE:
                    System.out.println("-> Influence declaration: " + action + ".\n");
                    /*
                      * Declares influence.
                      * Checks if awaiting action has changed control of acting Minister.
                      * If so, cancels awaiting action.
                    */
                    this.executeDeclareInfluence(action);
                    break;
                case SEND_HOSPITAL:
                    System.out.println("-> Sending Politico to Hospital: " + action + ".\n");
                    /* 
                        Changes GamePolitico's status to "AT_HOSPITAL"
                    */
                    this.gamePolService.sendToHospital(action.getTargetGamePolitico());
                    action.setStatus(ActionStatus.RESOLVED);
                    action.setResolved(true);
                    this.repoAction.save(action);

                    // Notify
                    eventType = SseEventType.ACTION_EXECUTED;
                    message = "Target Politico " + action.getTargetGamePolitico() +
                              " has been sent to the Hospital.\n";
                    this.notificationService.notifyAllPlayers(
                        action.getGame(), 
                        eventType, 
                        message
                    );
                    break;
                case EXIT_HOSPITAL:
                    System.out.println("-> Politico exiting Hospital: " + action + ".\n");
                    /*
                        Changes GamePolitico's status to "ACTIVE"
                    */
                    this.gamePolService.exitHospital(action.getTargetGamePolitico());
                    action.setStatus(ActionStatus.RESOLVED);
                    action.setResolved(true);
                    this.repoAction.save(action);

                    // Notify
                    eventType = SseEventType.ACTION_EXECUTED;
                    message = "Target Politico " + action.getTargetGamePolitico() +
                              " has been sent to the Hospital.\n";
                    this.notificationService.notifyAllPlayers(
                        action.getGame(), 
                        eventType, 
                        message
                    );

                    break;
                case PURGE_ATTEMPT:
                    /* Already validated and authorized */
                    /*
                        Performs purge roll and resolves if target is sent to Siberia.
                    */
                    this.executePurgeAction(action);
                    break;
                case EXILE_ESCAPE:
                    /* Already validated and authorized */
                    /*
                        Sends target GamePolitico on exile and cancels awaiting Action
                    */
                    this.executeExileEscape(action);
                    break;
                case EXILE_RETURN:
                    /* Already validated and authorized */
                    /*
                        Returns target GamePolitico from Exile and discards half of declared influence
                    */
                    this.executeExileReturn(action);
                    break;
                case BEGIN_INVESTIGATION:
                    /* Already validated and authorized */
                    /*
                        Adds an investigation to target GamePolitico
                    */
                    this.executeBeginInvestigation(action);
                    break;
                case REMOVE_INVESTIGATION:
                    /* Already validated and authorized */
                    /*
                        Removes an investigation from target GamePolitico.
                    */
                    this.executeRemoveInvestigation(action);
                    break;
                case CONDEMNATION:
                    /* Already validated and authorized */
                    /*
                        Sends a CANDIDATE directly to Siberia.
                    */
                   this.executeCondemnation(action);
                    break;
                case NEGATE_CONDEMNATION:
                    /* Already validated and authorized */
                    /*
                     * Cancels an awaiting Condemnation
                    */
                    this.executeNegateCondemnation(action);
                    break;
                default:
                    break;
            }
            // Set event type
            eventType = SseEventType.ACTION_EXECUTED;
            
        } catch (Exception e) {

            // Marcamos "action" como FAILED;
            action.setStatus(ActionStatus.FAILED);
            action.setResolved(false);
            this.repoAction.save(action);
            System.out.println("Action failed during resolution.\n");
            // Set event type
            eventType = SseEventType.ACTION_FAILED;
        }

        // Notify
        this.notificationService.broadcastGameUpdate(
            action.getGame(),
            eventType.name()
        );
    }

    /* Execute influence declaration */
    @Transactional
    private void executeDeclareInfluence(ActionInstance declareAction) {
        /* Check if influence declaration interrupts any awaiting action */

        Game game = declareAction.getGame();
        GamePolitico affected = declareAction.getTargetGamePolitico();
        ActionInstance awaitingAction = game.getCurrentAwaitingAction();
        boolean existsAwaiting = !game.getBlockingStatus().equals(ActionBlockingStatus.NONE)
                                 && awaitingAction != null;
        Player previousController = null;
        Player newController = null;
                    
        // If there is an awaiting action
        if (existsAwaiting) {
            // Get actual controller of acting Minister
            previousController = 
                this.controlService.getControllingPlayer(
                    awaitingAction.getActingGamePolitico().getId()
                );
        }

        // Build DeclaredRequestDTO from values in "action".
        DeclaredRequestDTO declareRequest = new DeclaredRequestDTO();
        declareRequest.setGamePoliticoId(declareAction.getTargetGamePolitico().getId());
        declareRequest.setPlayerId(declareAction.getActor().getId());
        declareRequest.setPoints(declareAction.getInfluencePoints());
        // Execute with DeclaredInfluenceService
        this.declareService.declareInfluence(declareRequest);

        // Message for succesful influence declaration.
        String declareMessage = "Player " + declareAction.getActor().getName() + 
                               " has succesfully declared " + 
                        declareAction.getInfluencePoints() + " influence points on " +
                        declareAction.getTargetGamePolitico().getPolitico().getName() +
                        ".\n";
        String awaitingMessage = "";

        // Check if awaiting action has changed acting Minister's controller
        if (existsAwaiting) {
            // New controller of awaitingAction's acting GamePolitico
            newController = this.controlService.getControllingPlayer(
                                awaitingAction.getActingGamePolitico().getId()
                            );
            // If trial in progress, check if affected Politico is a voter, and cancel vote
            if (game.getBlockingStatus() == ActionBlockingStatus.AWAITING_TRIAL_VOTES) {
                Trial trial = awaitingAction.getTrial();
                this.trialService.findVoteByVoter(trial, affected)
                                 .ifPresent(vote -> {
                                    this.trialService.cancelVote(trial, vote);
                                 });
            }
            // If awaitingAction's controller has changed, cancel awatingAction
            if (previousController != null &&
                newController != null &&
                !previousController.equals(newController)
            ) {
                // If awaitingAction is a trial, cancel and eliminate its Trial from it.
                if (awaitingAction.getType().equals(ActionType.OPEN_TRIAL)) {
                    Trial currenTrial = awaitingAction.getTrial();
                    currenTrial.setStatus(TrialStatus.CANCELLED);
                    this.repoTrial.save(currenTrial);
                    awaitingAction.setTrial(null);
                }
                // Update game- and awaitingAction's states and save
                awaitingAction.setStatus(ActionStatus.CANCELLED);
                this.repoAction.save(awaitingAction);
                game.setBlockingStatus(ActionBlockingStatus.NONE);
                game.setCurrentAwaitingAction(null);
                // Concatenate cancelled awaiting action's message
                awaitingMessage = "Awaiting action " + awaitingAction.getType() +
                                 " has been cancelled by control change of acting Minister.\n";
                declareMessage = declareMessage + awaitingMessage;
            }
        }

        // Resolve declaration and save all
        declareAction.setStatus(ActionStatus.RESOLVED);
        declareAction.setResolved(true);
        this.repoAction.save(declareAction);
        this.repoGame.save(game);

        // Notify
        this.notificationService.notifyAllPlayers(
            game,
            SseEventType.GAME_MESSAGE, 
            declareMessage
        );
    }

    /* Exile escape execution */
    @Transactional
    private void executeExileEscape(ActionInstance exileEscapeAction) {

        Game game = exileEscapeAction.getGame();
        ActionInstance currentAwaitingAction = game.getCurrentAwaitingAction();
        GamePolitico targetGamePolitico = exileEscapeAction.getTargetGamePolitico();

        // Update target Politico state
        targetGamePolitico.setStatus(GamePoliticoStatus.IN_EXILE);
        // Remove all investigations
        targetGamePolitico.setInvestigationCount(0);
        // Remove all damage
        targetGamePolitico.setDamage(0);
        // Remove minister (Ministry vacant, target Politico with gameMinistry = null)
        this.gameMinService.removeMinister(targetGamePolitico.getGameMinistry().getId());
        // Save GamePolitico
        this.repoGamePolitico.save(targetGamePolitico);

        // If awaiting action is a Trial, cancel it
        if (currentAwaitingAction.getType().equals(ActionType.OPEN_TRIAL)) {
            Trial currenTrial = currentAwaitingAction.getTrial();
            currenTrial.setStatus(TrialStatus.CANCELLED);
            this.repoTrial.save(currenTrial);
            currentAwaitingAction.setTrial(null);
        }
        // Cancel awaiting action
        currentAwaitingAction.setStatus(ActionStatus.CANCELLED);
        // Exile action resolved
        exileEscapeAction.setStatus(ActionStatus.RESOLVED);
        exileEscapeAction.setResolved(true);
        // Save both actions
        this.repoAction.save(currentAwaitingAction);
        this.repoAction.save(exileEscapeAction);
        
        // Update Game's states
        game.setCurrentAwaitingAction(null);
        game.setBlockingStatus(ActionBlockingStatus.NONE);
        // Save Game
        Game savedGame = this.repoGame.save(game);

        // Notify
        String targetGamePoliticoName = exileEscapeAction.getTargetGamePolitico()
                                                   .getPolitico().getName();
        String currentAwaitingActionName = currentAwaitingAction.getType().name();
        String actingMinisterName = currentAwaitingAction.getActingGamePolitico()
                                                         .getPolitico().getName();
        
        String message = "Target Politico " + targetGamePoliticoName +
                        " has succesfully gone into Exile.\n" + 
                        "Awaiting action " + currentAwaitingActionName +
                        " announced by " + actingMinisterName +
                        " has been cancelled by Exile escape.\n";
        
        // this.notificationService.broadcastGameUpdate(game, eventType.name());
        this.notificationService.notifyAllPlayers(
            savedGame,
            SseEventType.GAME_MESSAGE,
            message
        );
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.AWAITING_ACTION_CANCELLED,
            Map.of(
                "cancelledAction", currentAwaitingActionName
            )
        );
    }

    @Transactional
    private void executeExileReturn(ActionInstance exileReturnAction) {
        Game game = exileReturnAction.getGame();
        Player controller = exileReturnAction.getActor();
        GamePolitico targetGamePolitico = exileReturnAction.getTargetGamePolitico();

        /* --- Influence cost --- */
        InfluenceDeclared declared =
            this.declareService.getDeclaredByPlayerAndGamePolitico(
                controller, targetGamePolitico
            );
        // Calculate influence cost
        int cost = declared.getPoints() / 2;
        if (cost < 1) cost = 1;
        // Discard declared influence (discard value = cost)
        this.declareService.discardPartialDeclaredInfluence(declared, cost);

        // Update target Politico state
        targetGamePolitico.setStatus(GamePoliticoStatus.ACTIVE);
        // Update target Politico Ministry (first available PEOPLE)
        GameMinistry firstAvaliablePeopleMin =
            this.gameMinService.loopPeopleMinistries(game).get(0);
        firstAvaliablePeopleMin.setMinister(targetGamePolitico);
        targetGamePolitico.setGameMinistry(firstAvaliablePeopleMin);
        // Save returned Politico and PEOPLE Ministry
        this.gamePolService.saveGamePolitico(targetGamePolitico);
        this.gameMinService.saveGameMinistry(firstAvaliablePeopleMin);
        
        // Resolve action
        exileReturnAction.setStatus(ActionStatus.RESOLVED);
        exileReturnAction.setResolved(true);
        // Save action
        this.repoAction.save(exileReturnAction);

        // Notify
        String targetGamePoliticoName = exileReturnAction.getTargetGamePolitico()
                                                         .getPolitico().getName();
        String controllerName = controller.getName();
        String message = "Target Politico " + targetGamePoliticoName +
                        " has succesfully returned from Exile.\n" + 
                        "The cost of " + cost + " influence points declared by " + 
                        controllerName + " have been permanently discarded.\n" +
                        "The returned Politico has been placed within the ranks of the People.\n";
        
        this.notificationService.notifyAllPlayers(
            game,
            SseEventType.GAME_MESSAGE,
            message
        );
    }

    /* Execute Purge resolution */
    @Transactional
    private void executePurgeAction(ActionInstance purgeAction) {
        /*
            * Validate phase and authorize purge (done before execution)
            * Dice roll
            * Resolve roll
            * Perform effects according to result
        */
        Game game = purgeAction.getGame();
        GamePolitico actingMinister = purgeAction.getActingGamePolitico();
        GamePolitico targetGamePolitico = purgeAction.getTargetGamePolitico();
        GameMinistry targetMinistry = targetGamePolitico.getGameMinistry();
        String message = "";
        Game savedGame;

        // Calculate purge threshold
        int basePurgeValue = targetMinistry.getMinistry().getPurgeNr();
        int purgeModifier = 0;
        if (this.gamePolService.isInAdvantage(targetGamePolitico)) {
            purgeModifier = 2;
        }
        if (this.gamePolService.isInDisadvantage(targetGamePolitico)) {
            purgeModifier = -2;
        }
        int hospitalPenalty = 
            targetGamePolitico.getStatus().equals(GamePoliticoStatus.AT_HOSPITAL) ? -3 : 0;
        int purgeThreshold = 
            basePurgeValue + purgeModifier + hospitalPenalty;

        // Roll die
        int roll = this.diceService.rollSingle(20);

        // Resolve and apply effects
        if (roll >= purgeThreshold) {
            
            /** SUCCESFUL PURGE **/
            
            /* Vacant target Ministry */
            this.gameMinService.removeMinister(targetMinistry.getId());
            
            /* Send Minister to Siberia */
            this.gamePolService.sendToSiberia(targetGamePolitico);
            
            /* Find max declared value and delete its respective declaration (if found). */
            InfluenceDeclared maxDeclared = 
                this.declareService.getMaxDeclaredOnGamePolitico(targetGamePolitico);

            if (maxDeclared != null) {
                // Deletes Declared Influence
                // Substracts value from previous Assigned value
                // (if new Assigned value == 0, deletes Assigned Influence too)
                int discardedValue = maxDeclared.getPoints();
                InfluenceAssigned modified = 
                    this.declareService.discardAllDeclaredInfluence(maxDeclared);
                
                /***********************************************************/
                /* DEBUG */
                if (modified != null) {
                    int newValue = modified.getPoints();    /* DEBUG */
                    System.out.println("Discarded declared value: " + discardedValue + " points.\n");
                    System.out.println("New assigned value: " + newValue + " points.\n");
                }
                /***********************************************************/
            }

            // Resolve and save action
            purgeAction.setStatus(ActionStatus.RESOLVED);
            purgeAction.setResolved(true);
            this.repoAction.save(purgeAction);
            savedGame = this.repoGame.save(game);
            
            // Notifications for successful purge
            message = "Roll: " + roll + " beats minister's threshold: " + purgeThreshold +
                     ".\nTarget objective " + targetGamePolitico.getPolitico().getName() + 
                     " has been purged successfully and sent to Siberia.\n" + 
                     "All investigations on him/her have been discarded.\n" + 
                     "Max declared influence on him/her has been discarded.\n" +
                     "KGB Minister able to attempt new purge.\n";

        } else {

            /** FAILED PURGE **/

            // KGB Minister ages 3 years
            actingMinister.setCurrentAge(actingMinister.getCurrentAge() + 3);
            this.repoGamePolitico.save(actingMinister);
            // Mark purge as a FAILED action
            purgeAction.setStatus(ActionStatus.FAILED);
            // Save action state
            this.repoAction.save(purgeAction);            
            // This blocks new actions until end of phase
            game.setBlockingStatus(ActionBlockingStatus.FAILED_PURGE_BLOCK);
            // Save game state
            savedGame = this.repoGame.save(game);
            // Notification message for failed purge.
            message = "Roll: " + roll + " loses against minister's threshold: " + 
                     purgeThreshold + ".\n" + 
                     "Purge attempt failed. KGB Minister cannot purge again.\n" + 
                     "End of phase.\n";
        }

        // Notification message
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.GAME_MESSAGE, 
            message
        );
    }

    /* Execute Begin Investigation */
    @Transactional
    private void executeBeginInvestigation(ActionInstance action) {
        
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        GamePolitico actingGamePolitico = action.getActingGamePolitico();
        int currentInvestigations = targetGamePolitico.getInvestigationCount();
        
        // Update acting and target GamePoliticos' state
        targetGamePolitico.setInvestigationCount(currentInvestigations + 1);
        actingGamePolitico.setCurrentAge(actingGamePolitico.getCurrentAge() + 1);
        
        // Save all
        this.repoGamePolitico.save(targetGamePolitico);
        this.repoGamePolitico.save(actingGamePolitico);

        // Notify
        String message = "Acting Politico " + actingGamePolitico.getPolitico().getName() +
                        " has begun an investigation on " + 
                        targetGamePolitico.getPolitico().getName() + ".\n";
        this.notificationService.notifyAllPlayers(
            action.getGame(),
            SseEventType.GAME_MESSAGE,
            message
        );
    }

    /* Execute Remove Investigation */
    @Transactional
    private void executeRemoveInvestigation(ActionInstance action) {
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        GamePolitico actingGamePolitico = action.getActingGamePolitico();
        int currentInvestigations = targetGamePolitico.getInvestigationCount();
        
        // Update acting and target GamePoliticos' state
        targetGamePolitico.setInvestigationCount(currentInvestigations - 1);
        actingGamePolitico.setCurrentAge(actingGamePolitico.getCurrentAge() + 1);
        
        // Save all
        this.repoGamePolitico.save(targetGamePolitico);
        this.repoGamePolitico.save(actingGamePolitico);

        // Notify
        String message = "Acting Politico " + actingGamePolitico.getPolitico().getName() +
                        " has removed an investigation on " + 
                        targetGamePolitico.getPolitico().getName() + ".\n";
        this.notificationService.notifyAllPlayers(
            action.getGame(),
            SseEventType.GAME_MESSAGE,
            message
        );
    }

    /* Execute Condemnation */
    @Transactional
    private void executeCondemnation(ActionInstance action) {
        /*
            * Validated phase and authorized condemnation (in announceAction)
            * If not CANCELLED or responded with NEGATE_CONDEMNATION,
              send target Politico to Siberia
            * Acting Minister ages +2
            * All declared influence on target is discarded
            * All investigations on target are cleaned
        */

       // Validate if action is still awaiting (or has been corrupted somehow)
        Game game = action.getGame();
        ActionBlockingStatus awaitingStatus = game.getBlockingStatus();
        ActionInstance awaitingAction = game.getCurrentAwaitingAction();

        if (!awaitingStatus.equals(ActionBlockingStatus.AWAITING_CONDEMNATION_RESPONSE) ||
            !awaitingAction.equals(action)
        ) {
            // Just mark it as failed and skip it 
            // (an exception would break entire resolution chain).
            action.setStatus(ActionStatus.FAILED);
            action.setResolved(false);
            this.repoAction.save(action);
            System.out.println("Condemnation has failed. Internal server error.\n");
            return;
        }
        if ( !awaitingAction.getStatus().equals(ActionStatus.ANNOUNCED) ) {
            throw new IllegalStateException(
                "Invalid awaiting action status.\n"
            );
        }

        /* If reached here, execute */
        GamePolitico targetPolitico = action.getTargetGamePolitico();
        GamePolitico actingPolitico = action.getActingGamePolitico();
        GameMinistry targetMinistry = targetPolitico.getGameMinistry();
        
        // Discard all influence declared on target (by any Player)
        List<InfluenceDeclared> allDeclaredOnTarget = 
            this.declareService.getAllDeclaredOnGamePolitico(targetPolitico);
        for (InfluenceDeclared declared : allDeclaredOnTarget) {
            // Discard declared (saved in service method)
            this.declareService.discardAllDeclaredInfluence(declared);
        }

        // Clean all investigations on target
        targetPolitico.setInvestigationCount(0);
        targetPolitico.setInvestigationCountAtPhaseStart(0);
        targetPolitico.setImmuneToInvestigationsUntilTurn(null);
        // Un-assign Ministry
        targetPolitico.setGameMinistry(null);
        // Vacant CANDIDATE "ministry"
        targetMinistry.setMinister(null);
        targetMinistry.setVacant(true);
        // Save vacant Ministry's state
        this.gameMinService.saveGameMinistry(targetMinistry);
        // Send target to Siberia
        targetPolitico.setStatus(GamePoliticoStatus.IN_SIBERIA);
        // Save target Politico's status
        this.gamePolService.saveGamePolitico(targetPolitico);

        // Acting Minister ages +2
        int ministerAge = actingPolitico.getCurrentAge();
        actingPolitico.setCurrentAge(ministerAge + 2);
        this.gamePolService.saveGamePolitico(actingPolitico);

        // Resolve action
        action.setStatus(ActionStatus.RESOLVED);
        action.setResolved(true);
        ActionInstance savedAction = this.repoAction.save(action);

        // Clean game's awaiting action and blocking status
        game.setBlockingStatus(ActionBlockingStatus.NONE);
        game.setCurrentAwaitingAction(null);
        Game savedGame = this.repoGame.save(game);

        // Notify
        String targetPoliticoName = targetPolitico.getPolitico().getName();
        String actingMinisterName = actingPolitico.getPolitico().getName();
        String message = "Target Politico " + targetPoliticoName +
                        " has been succesfully Condemned to Siberia by Minister "
                        + actingMinisterName + ".\n" +
                        "All investigations on target are cleaned, and all influence declared on him/her by any Faction has been discarded.\n";
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.AWAITING_ACTION_RESOLVED,
            Map.of(
                "actionType", savedAction.getType().name(),
                "result", savedAction.getStatus().name() 
            )
        );
        this.notificationService.broadcastGameUpdate(
            savedGame,
            SseEventType.AWAITING_ACTION_RESOLVED.name()
        );
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.GAME_MESSAGE, 
            message
        );
    }

    /* Execute Negate Condemnation */
    @Transactional
    private void executeNegateCondemnation(ActionInstance negateAction) {
        
        Game game = negateAction.getGame();
        ActionInstance awaitingAction = game.getCurrentAwaitingAction();
        GamePolitico actingMinister = awaitingAction.getActingGamePolitico();
        GamePolitico negatorMinister = negateAction.getActingGamePolitico();
        int currentAge = actingMinister.getCurrentAge();

        // Age negator minister +5 and save
        negatorMinister.setCurrentAge(currentAge + 5);
        this.gamePolService.saveGamePolitico(negatorMinister);

        // Cancel Condemnation
        awaitingAction.setStatus(ActionStatus.CANCELLED);
        awaitingAction.setResolved(true);
        // Resolve Negate Condemnation
        negateAction.setStatus(ActionStatus.RESOLVED);
        negateAction.setResolved(true);
        // Save both actions
        this.repoAction.save(awaitingAction);
        this.repoAction.save(negateAction);

        // Clear awaiting action block
        game.setBlockingStatus(ActionBlockingStatus.NONE);
        game.setCurrentAwaitingAction(null);
        // Save Game
        Game savedGame = this.repoGame.save(game);

        // Notify
        String targetGamePoliticoName = negateAction.getTargetGamePolitico()
                                                    .getPolitico().getName();
        String awaitingActionName = awaitingAction.getType().name();
        String actingMinisterName = actingMinister.getPolitico().getName();
        String negatorMinisterName = negatorMinister.getPolitico().getName();
        String message = "Target Politico " + targetGamePoliticoName +
                        " has succesfully been saved from a Condemnation by " +
                        negatorMinisterName + ", who has aged 5 years.\n" +  
                        "Awaiting action " + awaitingActionName +
                        " announced by " + actingMinisterName +
                        " has been cancelled by Negate Condemnation.\n";

        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.AWAITING_ACTION_CANCELLED, 
            Map.of(
                "cancelledAction", awaitingActionName,
                "byAction", negateAction.getType().name()
            )
        );
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.GAME_MESSAGE,
            message
        );
        this.notificationService.broadcastGameUpdate(
            savedGame, 
            SseEventType.AWAITING_ACTION_CANCELLED.name()
        );
    }

    /* Resolve an existing awaiting Action */
    public Game resolveAwaitingAction(Game game) {

        ActionBlockingStatus blockingStatus = game.getBlockingStatus();
        ActionInstance awaitingAction = game.getCurrentAwaitingAction();
        // Validate awaiting action status
        if (blockingStatus.equals(ActionBlockingStatus.NONE) ||
            awaitingAction == null ||
            !awaitingAction.getStatus().equals(ActionStatus.ANNOUNCED)
        )
        {
            throw new IllegalStateException(
                "Invalid state and/or awaiting action not found.\n"
            );
        }

        switch (awaitingAction.getType()) {
            case PURGE_ATTEMPT:
                this.executeAction(awaitingAction);
                break;
            case CONDEMNATION:
                this.executeAction(awaitingAction);
                break;
            default:
                throw new IllegalStateException(
                    "Unsupported awaiting action type: " + awaitingAction.getType() + ".\n"
                );
        }

        game.clearReadyPlayers();
        Game saved = repoGame.save(game);

        return saved;
    }

    /* Resolve pending (ANNOUNCED) actions */
    @Transactional
    public void resolvePendingActions(Long game_id) {
        
        Game game = this.validationService.validateGameByID(game_id);

        // Check if there's a blocking action awaiting resolution
        if (game.getBlockingStatus() != ActionBlockingStatus.NONE) {
            // If action is still active (ANNOUNCED)
            ActionInstance awaitingAction = game.getCurrentAwaitingAction();
            if (awaitingAction != null && awaitingAction.getStatus() == ActionStatus.ANNOUNCED) {
                // Unblock state to accept new actions
                game.setBlockingStatus(ActionBlockingStatus.NONE);
                // Clean awaiting action
                game.setCurrentAwaitingAction(null);
                // Save game state
                this.repoGame.save(game);
            }
        }

        // Rest of announced actions for this game
        List<ActionInstance> pending = this.repoAction.findByGameAndStatus(game, ActionStatus.ANNOUNCED);
        System.out.println(pending);

        // Sort by priority
        pending.sort(
            Comparator.comparing(ActionInstance::getPriority)
                      .thenComparing(ActionInstance::getCreatedAt)
        );
        
        // Execute actions in order of priority
        for (ActionInstance action : pending) {
            this.executeAction(action);
        }
    }

    // Getter
    public List<ActionInstanceDTO> getPendingActions(Long gameID) {
        Game game = this.validationService.validateGameByID(gameID);
        List<ActionInstance> pendingActions = new ArrayList<>();
        pendingActions = this.repoAction.findByGameAndStatus(game, ActionStatus.ANNOUNCED);
        return pendingActions.stream()
                             .map( action -> this.actionMapper.toDTO(action) )
                             .collect(Collectors.toList());
    }

    // Getter
    public List<ActionInstanceDTO> getActionsByPlayer(Long game_id, Long player_id) {
        Game game = this.repoGame.findById(game_id).orElse(null);
        Player player = this.repoPlayer.findById(player_id).orElse(null);
        if (game == null || player == null) {
            System.out.println("El ID de la Partida o del Jugador no es válido.\n");
            return null;
        }

        List<ActionInstance> playerActions = this.repoAction.findByGameAndActor(game, player);
        return playerActions.stream()
                            .map(action -> this.actionMapper.toDTO(action))
                            .collect(Collectors.toList());
    }

    // Getter
    public Set<ActionType> getPossibleActionsByPhase(PhaseType phaseType) {
        return ActionType.fromOrder(phaseType.getOrder());
    }

}
