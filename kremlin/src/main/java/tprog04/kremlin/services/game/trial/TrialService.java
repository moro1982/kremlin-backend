package tprog04.kremlin.services.game.trial;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import tprog04.kremlin.aux_classes.ActionBlockingStatus;
import tprog04.kremlin.aux_classes.ActionStatus;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;
import tprog04.kremlin.aux_classes.MinistryEnum;
import tprog04.kremlin.aux_classes.SseEventType;
import tprog04.kremlin.aux_classes.TrialResult;
import tprog04.kremlin.aux_classes.TrialStatus;
import tprog04.kremlin.aux_classes.TrialVoteValue;
import tprog04.kremlin.models.ActionInstance;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.InfluenceDeclared;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.models.Trial;
import tprog04.kremlin.models.TrialVote;
import tprog04.kremlin.repositories.ActionInstanceRepository;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.TrialRepository;
import tprog04.kremlin.services.ControlPoliticoService;
import tprog04.kremlin.services.GameMinistryService;
import tprog04.kremlin.services.GamePoliticoService;
import tprog04.kremlin.services.influence.implementations.DeclaredInfluenceService;
import tprog04.kremlin.services.notification.PlayerNotificationService;
import tprog04.kremlin.services.validation.ValidationService;

@Service
public class TrialService {
    @Autowired
    private TrialRepository repoTrial;
    @Autowired
    private GameRepository repoGame;
    @Autowired
    private ActionInstanceRepository repoAction;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private PlayerNotificationService notificationService;
    @Autowired
    private GameMinistryService gameMinService;
    @Autowired
    private GamePoliticoService gamePolService;
    @Autowired
    private ControlPoliticoService controlService;
    @Autowired
    private DeclaredInfluenceService influenceService;

    public Trial saveTrial( Trial trial ) {
        Trial saved = this.repoTrial.save(trial);
        return saved;
    }

    public Game beginTrial( Game game ) {
        
        // Validate awaiting action
        this.validationService.validateGameHasAwaitingAction(game);
        // Validate awaiting action is OPEN_TRIAL and has Trial attached
        ActionInstance awaitingAction = game.getCurrentAwaitingAction();
        Trial trial = awaitingAction.getTrial();
        if (!awaitingAction.getType().equals(ActionType.OPEN_TRIAL) || trial == null) {
            throw new IllegalStateException(
                "Action is not OPEN_TRIAL or has no Trial attached.\n"
            );
        }
        // Validate correct Game blockingStatus
        if (!game.getBlockingStatus().equals(ActionBlockingStatus.AWAITING_TRIAL_RESPONSE)) {
            throw new IllegalStateException(
                "Invalid game state.\n"
            );
        }

        trial.setStatus(TrialStatus.VOTING);
        Trial savedTrial = this.repoTrial.save(trial);
        
        game.setBlockingStatus(ActionBlockingStatus.AWAITING_TRIAL_VOTES);
        game.setTrial(savedTrial);
        Game savedGame = this.repoGame.save(game);

        // Notify
        Set<Long> eligibleVotersIDs = this.getEligibleVoters(savedGame)
                                            .stream()
                                            .map( gp -> gp.getId())
                                            .collect(Collectors.toSet());
        String message = "Opened Trial has begun. Awaiting voters to cast their vote.\n";
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.TRIAL_BEGIN_VOTING,
            Map.of(
                "accusedPoliticoID", savedTrial.getAccused().getId(),
                "eligibleVoters", eligibleVotersIDs
            )
        );
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.GAME_MESSAGE, 
            message
        );

        return savedGame;
    }

    public void validateTrialAwaitingResponse( Game game ) {
        // Validate awaiting Action -> AWAITING_TRIAL_RESPONSE
        ActionInstance currentAwaitingAction = game.getCurrentAwaitingAction();
        if (!game.getBlockingStatus().equals(ActionBlockingStatus.AWAITING_TRIAL_RESPONSE) ||
            currentAwaitingAction == null
        ) {
            throw new IllegalStateException("There must be a Trial awaiting responses.\n");
        }
        // Validate Trial -> AWAITING_RESPONSE
        Trial trial = currentAwaitingAction.getTrial();
        if ( trial == null || trial.getStatus() != TrialStatus.AWAITING_RESPONSE) {
            throw new IllegalStateException("There must be a Trial awaiting responses.\n");
        }
    }

    public void validateTrialAwaitingVotes( Game game ) {
        // Validate awaiting Action -> AWAITING_TRIAL_VOTES
        ActionInstance currentAwaitingAction = game.getCurrentAwaitingAction();
        if (!game.getBlockingStatus().equals(ActionBlockingStatus.AWAITING_TRIAL_VOTES) ||
            currentAwaitingAction == null
        ) {
            throw new IllegalStateException("There must be a Trial awaiting votes.\n");
        }
        // Validate Trial -> VOTING
        Trial trial = currentAwaitingAction.getTrial();
        if ( trial == null || trial.getStatus() != TrialStatus.VOTING) {
            throw new IllegalStateException("There must be a Trial awaiting votes.\n");
        }
    }
    
    public void validateAccusedOrProsecutor( Game game, GamePolitico gamePol ) {
        GamePolitico accused = game.getTrial().getAccused();
        GamePolitico prosecutor = game.getTrial().getProsecutor();
        if ( accused.equals(gamePol) || prosecutor.equals(gamePol) ) {
            throw new IllegalStateException(
                "Accused always votes INNOCENT. Prosecutor always votes GUILTY.\n"
            );
        }
    }

    public void validateAlreadyVoted( Game game, GamePolitico gamePol ) {
        Trial trial = game.getTrial();
        for (TrialVote vote : trial.getVotes()) {
            if (vote.getVoter().equals(gamePol)) {
                throw new IllegalStateException(
                    "The voter has already cast his/her vote.\n"
                );
            }
        }
    }

    public void validatePlayerHasVotedAll( Game game, Player player ) {
        Trial trial = game.getTrial();
        Set<GamePolitico> voters = this.getEligibleVoters(game);
        Set<GamePolitico> controlledByPlayer = 
            voters.stream()
                  .filter(gamePol -> this.controlService.getControllingPlayer(gamePol.getId())
                                                        .equals(player))
                  .collect(Collectors.toSet());
        for (GamePolitico gamePol : controlledByPlayer) {
            if (this.findVoteByVoter(trial, gamePol).isEmpty()) {
                throw new IllegalStateException(
                    "Player has not voted with all controlled voters.\n"
                );
            }
        }
    }

    public Set<GamePolitico> getEligibleVoters( Game game ) {
        
        List<MinistryEnum> ministriesAllowedToVote = 
            this.gameMinService.getAllowedMinistriesByActionType(ActionType.CAST_TRIAL_VOTE);
        Set<GamePolitico> allGamePoliticos = game.getGamePoliticos();

        Set<GamePolitico> allegedlyAllowedToVote = 
            allGamePoliticos.stream()
                            .filter(gamePol -> ministriesAllowedToVote.contains(
                                gamePol.getGameMinistry().getMinistry().getName()
                            ))
                            .collect(Collectors.toSet());

        Set<GamePolitico> actuallyAllowedToVote = 
            allegedlyAllowedToVote.stream()
                                  .filter(gamePol -> gamePol.getStatus().equals(GamePoliticoStatus.ACTIVE))
                                  .collect(Collectors.toSet());

        return actuallyAllowedToVote;
    }

    public boolean allVotesCast( Game game ) {
        
        Trial trial = game.getTrial();

        Set<GamePolitico> gamePoliticosAllowedToVote = this.getEligibleVoters(game);

        for (GamePolitico voter : gamePoliticosAllowedToVote) {
            // Check if Politico has cast his/her vote
            boolean hasVote = 
                trial.getVotes().stream()
                                .anyMatch(v -> v.getVoter().equals(voter));

            // Uncontrolled Politicos have fixed vote (INNOCENT)
            // (to be resolved in the end)
            boolean uncontrolled = 
                this.controlService.getControllingPlayer(voter.getId()) == null;

            if (!hasVote && !uncontrolled) {
                return false;
            }
        }

        return true;
    }

    public boolean allVotersReady(Game game) {
        
        Set<GamePolitico> eligibleVoters = this.getEligibleVoters(game);

        Set<Player> playersThatMustBeReady = 
            eligibleVoters.stream()
                          .map(gp -> controlService.getControllingPlayer(gp.getId()))
                          .filter(Objects::nonNull)
                          .collect(Collectors.toSet());
        
        return game.getReadyPlayers().containsAll(
            playersThatMustBeReady.stream()
                                  .map(Player::getId)
                                  .collect(Collectors.toSet())   
        );
    }

    public Optional<TrialVote> findVoteByVoter( Trial trial, GamePolitico voter ) {
        return trial.getVotes().stream()
                               .filter(vote -> vote.getVoter().equals(voter))
                               .findFirst();
    }

    public void cancelVote( Trial trial, TrialVote vote ) {
        vote.setCancelled(true);
        trial.getVotes().remove(vote);
    }

    @Transactional
    public Game resolveTrial( Game game ) {

        this.validateTrialAwaitingVotes(game);

        Trial trial = game.getTrial();
        trial.setStatus(TrialStatus.COUNTING);

        TrialResult result = this.countVotes(game);

        if (result == TrialResult.ABSOLVED) {
            this.resolveAbsolution(game);
        } else {
            this.resolveConviction(game);
        }

        trial.setStatus(
            result == TrialResult.ABSOLVED
                    ? TrialStatus.RESOLVED_ABSOLVED
                    : TrialStatus.RESOLVED_CONVICTED
        );

        Trial savedTrial = this.saveTrial(trial);
        Game savedGame = this.repoGame.save(game);

        // Notify
        this.notificationService.notifyAllPlayers(
            savedGame, 
            SseEventType.TRIAL_RESOLVED, 
            Map.of(
                "verdict", savedTrial.getStatus().name(),
                "accusedPoliticoID", savedTrial.getAccused().getId()
            )
        );

        this.cleanupAfterTrial(game);
        savedGame = this.repoGame.save(savedGame);

        // Notify
        this.notificationService.broadcastGameUpdate(
            savedGame, 
            SseEventType.TRIAL_RESOLVED.name()
        );

        return savedGame;
    }

    public TrialResult countVotes( Game game ) {
        
        Trial trial = game.getTrial();
        int innocentVotes = 0;

        GamePolitico accused = trial.getAccused();

         /* AUTOMATIC ACCUSED VOTE */
        if (accused.getStatus() == GamePoliticoStatus.ACTIVE) {
            innocentVotes++; // automatic INNOCENT
        }

        /* PROSECUTOR'S VOTE NOT COUNTED (always GUILTY) */

        /* EXPLICIT VOTES */
        for (TrialVote vote : trial.getVotes()) {

            if (vote.isCancelled()) continue;

            if (vote.getVote() == TrialVoteValue.INNOCENT) {
                innocentVotes++;
            }

            /* early exit */
            if (innocentVotes >= 2) {
                return TrialResult.ABSOLVED;
            }
        }

        /* IF innocentVotes < 2 → CONVICTED */
        return TrialResult.CONVICTED;
    }

    @Transactional
    public void cleanupAfterTrial( Game game ) {
        
        ActionInstance awaitingAction = game.getCurrentAwaitingAction();
        awaitingAction.setStatus(ActionStatus.RESOLVED);
        awaitingAction.setResolved(true);
        this.repoAction.save(awaitingAction);

        game.setBlockingStatus(ActionBlockingStatus.NONE);
        game.setCurrentAwaitingAction(null);
        game.setTrial(null);
        game.clearReadyPlayers();
        this.repoGame.save(game);
    }

    @Transactional
    public void resolveAbsolution( Game game ) {
        
        Trial trial = game.getTrial();
        GamePolitico accused = trial.getAccused();
        GamePolitico prosecutor = trial.getProsecutor();

        /* Effects on Accused: */
            // Reset investigation count
            // Block new investigations until next turn (beginning of phase 3)
        accused.setInvestigationCount(0);
        accused.setImmuneToInvestigationsUntilTurn(game.getCurrentTurn() + 1);
        /* Effects on Prosecutor */
            // Age +3
        prosecutor.setCurrentAge(prosecutor.getCurrentAge() + 3);
        /* Save all */
        this.gamePolService.saveGamePolitico(accused);
        this.gamePolService.saveGamePolitico(prosecutor);
        /* Update trial status and save */
        trial.setStatus(TrialStatus.RESOLVED_ABSOLVED);
        this.repoTrial.save(trial);

        // Private notifications to affected Politicos' controllers
        Player accusedController = 
            this.controlService.getControllingPlayer(accused.getId());
        Player prosecutorController = 
            this.controlService.getControllingPlayer(prosecutor.getId());
        this.notificationService.notifySinglePlayer(
            game, 
            accusedController, 
            SseEventType.PRIVATE_MESSAGE, 
            Map.of( "controlledPoliticoAffected", accused.getId() )
        );
        this.notificationService.notifySinglePlayer(
            game, 
            prosecutorController, 
            SseEventType.PRIVATE_MESSAGE, 
            Map.of( "controlledPoliticoAffected", prosecutor.getId() )
        );
    }

    @Transactional
    public void resolveConviction( Game game ) {
        Trial trial = game.getTrial();
        GamePolitico accused = trial.getAccused();

        accused.setStatus(GamePoliticoStatus.IN_SIBERIA);
        accused.setInvestigationCount(0);
        this.gamePolService.saveGamePolitico(accused);

        InfluenceDeclared maxDeclared = 
            this.influenceService.getMaxDeclaredOnGamePolitico(accused);
        this.influenceService.discardAllDeclaredInfluence(maxDeclared);

        trial.setStatus(TrialStatus.RESOLVED_CONVICTED);
        this.repoTrial.save(trial);

        // Private notification to affected Politico's controller
        Player accusedController = 
            this.controlService.getControllingPlayer(accused.getId());
        this.notificationService.notifySinglePlayer(
            game, 
            accusedController, 
            SseEventType.PRIVATE_MESSAGE, 
            Map.of( "controlledPoliticoAffected", accused.getId() )
        );
    }

}
