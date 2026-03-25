package tprog04.kremlin.services.validation;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tprog04.kremlin.aux_classes.ActionBlockingStatus;
import tprog04.kremlin.aux_classes.ActionStatus;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;
import tprog04.kremlin.aux_classes.MinistryEnum;
import tprog04.kremlin.models.ActionInstance;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GameMinistry;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.InfluenceAssigned;
import tprog04.kremlin.models.InfluenceDeclared;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.repositories.ActionInstanceRepository;
import tprog04.kremlin.services.ControlPoliticoService;
import tprog04.kremlin.services.GameMinistryService;
import tprog04.kremlin.services.game.trial.TrialService;
import tprog04.kremlin.services.influence.implementations.AssignedInfluenceService;
import tprog04.kremlin.services.influence.implementations.DeclaredInfluenceService;

@Component
public class ActionAuthorizationService {
    
    @Autowired
    private ActionInstanceRepository repoAction;
    @Autowired
    private ControlPoliticoService controlPoliticoService;
    @Autowired
    private AssignedInfluenceService assignedService;
    @Autowired
    private DeclaredInfluenceService declaredService;
    @Autowired
    private GameMinistryService gameMinService;
    @Autowired
    private TrialService trialService;

    public void authorizeAnnounce(
        ActionType actionType,
        Player player,
        Game game,
        ActionInstance action
    )
    {
        switch (actionType) {
            case DECLARE_INFLUENCE:
                this.authorizeDeclareInfluence(player, game, action);
                break;
            case SEND_HOSPITAL:
                this.authorizeSendHospital(player, game, action);
                break;
            case EXIT_HOSPITAL:
                this.authorizeExitHospital(player, game, action);
                break;
            case PURGE_ATTEMPT:
                this.authorizePurge(player, game, action);
                break;
            case EXILE_ESCAPE:
                this.authorizeExileEscape(player, game, action);
                break;
            case EXILE_RETURN:
                this.authorizeExileReturn(player, game, action);
                break;
            case BEGIN_INVESTIGATION:
                this.authorizeBeginInvestigation(player, game, action);
                break;
            case REMOVE_INVESTIGATION:
                this.authorizeRemoveInvestigation(player, game, action);
                break;
            case OPEN_TRIAL:
                this.authorizeOpenTrial(player, game, action);
                break;
            case CAST_TRIAL_VOTE:
                this.authorizeCastTrialVote(player, game, action);
                break;
            case CONDEMNATION:
                this.authorizeCondemnation(player, game, action);
                break;
            case NEGATE_CONDEMNATION:
                this.authorizeNegateCondemnation(player, game, action);
                break;
            default:
                break;
        }
    }

    private void authorizeDeclareInfluence(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Check if declared value is valid.
        Integer influencePoints = action.getInfluencePoints();
        if ( influencePoints == null || influencePoints <= 0 ) {
            throw new IllegalStateException("Invalid influence value.\n");
        }

        // Validate if GamePolitico exists
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        if (targetGamePolitico == null) {
            throw new IllegalStateException("Target Politico is required.\n");
        }
        // Validate if GamePolitico belongs to this Game
        if (!targetGamePolitico.getGame().equals(game)) {
            throw new IllegalStateException("Politico doesn't belong to this game.\n");
        }
        // Validate if GamePolitico isn't in Exile
        if (targetGamePolitico.getStatus() == GamePoliticoStatus.IN_EXILE)
        {
            throw new IllegalStateException(
                "Influence cannot be declared on Politicos in Exile.\n"
            );
        }

        // Check if Player has influence assigned on target GamePolitico.
        InfluenceAssigned assigned = 
            this.assignedService.getAssignedByPlayerAndGamePolitico(player, targetGamePolitico);
        // Check if assigned influence is greater than declared value.
        if (assigned.getPoints() < influencePoints) {
            throw new IllegalStateException(
                "Declared influence exceeds assigned influence.\n"
            );
        }

        // Check if Player has influence declared on target GamePolitico.
        InfluenceDeclared prevDeclared =
            this.declaredService.getDeclaredByPlayerAndGamePolitico(player, targetGamePolitico);
        // Check if new declared influence value is greater than previous declared value.
        if (prevDeclared != null && prevDeclared.getPoints() >= influencePoints) {
            throw new IllegalStateException("Declared value must be greater than previous declared value.\n");
        }
    }

    private void authorizeSendHospital(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Validate if GamePolitico exists
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        if (targetGamePolitico == null) {
            throw new IllegalStateException("Target Politico is required.\n");
        }
        // Validate if GamePolitico belongs to this Game
        if (!targetGamePolitico.getGame().equals(game)) {
            throw new IllegalStateException("Politico doesn't belong to this game.\n");
        }
        // Check if Player controls target GamePolitico
        if (!this.controlPoliticoService.playerControlsPolitico(player, targetGamePolitico))
        {
            throw new IllegalStateException("Player doesn't control target Politico.\n");
        }
        
        // Check if GamePolitico can be sent to the Hospital
        if (targetGamePolitico.getStatus() != GamePoliticoStatus.ACTIVE)
        {
            throw new IllegalStateException("Target Politico cannot be sent to Hospital.\n");
        }
    }

    private void authorizeExitHospital(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Validate if GamePolitico exists
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        if (targetGamePolitico == null) {
            throw new IllegalStateException("Target Politico is required.\n");
        }
        // Validate if GamePolitico belongs to this Game
        if (!targetGamePolitico.getGame().equals(game)) {
            throw new IllegalStateException("Politico doesn't belong to this game.\n");
        }
        // Check if Player controls target GamePolitico
        if (!this.controlPoliticoService.playerControlsPolitico(player, targetGamePolitico))
        {
            throw new IllegalStateException("Player doesn't control target Politico.\n");
        }
        // Check if GamePolitico is at the Hospital
        if (targetGamePolitico.getStatus() != GamePoliticoStatus.AT_HOSPITAL)
        {
            throw new IllegalStateException("Target Politico is not at the Hospital.\n");
        }
    }

    private void authorizePurge(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Validate if there isn't another awaiting action in progress.
        if (game.getBlockingStatus() != ActionBlockingStatus.NONE ||
            game.getCurrentAwaitingAction() != null
        ) {
            throw new IllegalStateException(
                "Unable to announce action. Awaiting action in progress.\n");
        }

        // Validate if acting GamePolitico and target Politico exist
        GamePolitico actingGamePolitico = action.getActingGamePolitico();
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        if (actingGamePolitico == null || targetGamePolitico == null) {
            throw new IllegalStateException(
                "Acting Minister and target Politico required.\n"
            );
        }
        // Validate if both Politicos belong to this Game
        if (!actingGamePolitico.getGame().equals(game) || 
            !targetGamePolitico.getGame().equals(game)
        ) {
            throw new IllegalStateException(
                "Minister or Target Politico doesn't belong to this game.\n"
            );
        }

        // Validate if acting Politico is controlled by Player
        if (!this.controlPoliticoService.playerControlsPolitico(player, actingGamePolitico)) {
            throw new IllegalStateException("Player doesn't control acting Minister.\n");
        }
        // Validate if acting Politico is ACTIVE
        if (!actingGamePolitico.getStatus().equals(GamePoliticoStatus.ACTIVE)) {
            throw new IllegalStateException(
                "Acting Politico must be ACTIVE to perform an action.\n"
            );
        }
        
        // Validate if acting Ministry is allowed to perform this action this turn
        GameMinistry actingMinistry = actingGamePolitico.getGameMinistry();
        MinistryEnum actingMinistryName = actingMinistry.getMinistry().getName();
        MinistryEnum currentAuthorized = 
            this.gameMinService.resolveAuthorizedMinistryForActionType(action.getType(), game)
                                    .get(0);
        if (!actingMinistryName.equals(currentAuthorized)) {
            throw new IllegalStateException(
                "Acting Ministry is not allowed to perform this action.\n"
            );
        }
        // Validate if acting Politico is assigned to authorized Ministry
        if (!actingMinistry.getMinister().equals(actingGamePolitico)) {
            throw new IllegalStateException(
                "Acting Politico must be assigned to authorized Ministry.\n"
            );
        }

        // Validate if target Politico can be a purge target.
        MinistryEnum targetMinistryName = targetGamePolitico.getGameMinistry()
                                                            .getMinistry().getName();
        if (targetMinistryName.equals(actingMinistryName) || 
            targetMinistryName.equals(MinistryEnum.PEOPLE))
        {
            throw new IllegalStateException(
                "Acting Minister can't target him/herself or the People.\n"
            );
        }
        // Validate if target Politico is not in Siberia, in Exile or Inactive (Dead/Not in Game)
        if (targetGamePolitico.getStatus().equals(GamePoliticoStatus.IN_EXILE) ||
            targetGamePolitico.getStatus().equals(GamePoliticoStatus.IN_SIBERIA) ||
            targetGamePolitico.getStatus().equals(GamePoliticoStatus.INACTIVE)
        )
        {
            throw new IllegalStateException(
                "Politicos on Exile, in Siberia or Inactive can't be targeted.\n"
            );
        }
    }

    private void authorizeExileEscape(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Check if there is an awaiting action
        if (game.getBlockingStatus().equals(ActionBlockingStatus.NONE) ||
            game.getCurrentAwaitingAction() == null
        ) {
            throw new IllegalStateException("There must be an awaiting action to go into Exile.\n");
        }
        ActionInstance currentAwaitingAction = game.getCurrentAwaitingAction();
        ActionType awaitingType = currentAwaitingAction.getType();
        ActionBlockingStatus status = game.getBlockingStatus();
        boolean valid = (awaitingType == ActionType.PURGE_ATTEMPT &&
                         status == ActionBlockingStatus.AWAITING_PURGE_RESPONSE)
                        ||
                        (awaitingType == ActionType.OPEN_TRIAL &&
                         status == ActionBlockingStatus.AWAITING_TRIAL_RESPONSE);
        // Check if awaiting action is a Purge or a Trial
        if ( !valid ) {
            throw new IllegalStateException(
                "Exile Escape can only be announced immediately after a Purge or Trial announcement.\n");
        }
        // Check awaiting action's status (must be ANNOUNCED)
        if (!currentAwaitingAction.getStatus().equals(ActionStatus.ANNOUNCED)) {
            throw new IllegalStateException(
                "Invalid awaiting action status.\n"
            );
        }

        GamePolitico exileTarget = action.getTargetGamePolitico();
        GamePolitico awaitingActionTarget = currentAwaitingAction.getTargetGamePolitico();
        // Check if awaiting action's target is the same as exile's target
        if (!awaitingActionTarget.equals(exileTarget)) {
            throw new IllegalStateException(
                "Exile's target Politico must be the target of current awaiting action.\n"
            );
        }
        // Check if Player controls awaiting action's target.
        if (!this.controlPoliticoService.playerControlsPolitico(player, awaitingActionTarget)) {
            throw new IllegalStateException(
                "Player must control awaiting action's target Politico.\n"
            );
        }
        
        // Check if Exile's target is able to go into Exile.
        if (exileTarget.getStatus().equals(GamePoliticoStatus.INACTIVE) ||
            exileTarget.getStatus().equals(GamePoliticoStatus.IN_SIBERIA) ||
            exileTarget.getStatus().equals(GamePoliticoStatus.IN_EXILE)
        ) {
            throw new IllegalStateException("Politicos in Exile, in Siberia or Inactive can't go on Exile.\n");
        }
    }

    private void authorizeExileReturn(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Validate if target GamePolitico exists
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        if (targetGamePolitico == null) {
            throw new IllegalStateException("Target Politico is required.\n");
        }
        // Validate if target GamePolitico belongs to this Game
        if (!targetGamePolitico.getGame().equals(game)) {
            throw new IllegalStateException("Politico doesn't belong to this game.\n");
        }
        // Check if Player controls target GamePolitico
        if (!this.controlPoliticoService.playerControlsPolitico(player, targetGamePolitico))
        {
            throw new IllegalStateException("Player doesn't control target Politico.\n");
        }
        
        // Check if GamePolitico is IN_EXILE
        if (targetGamePolitico.getStatus() != GamePoliticoStatus.IN_EXILE)
        {
            throw new IllegalStateException("Target Politico must be in Exile.\n");
        }

        // Validate target Politico has declared influence
        InfluenceDeclared declared =
            this.declaredService.getDeclaredByPlayerAndGamePolitico(player, targetGamePolitico);

        if (declared == null || declared.getPoints() <= 0) {
            throw new IllegalStateException(
                "Declared influence required to return Politico from Exile.\n"
            );
        }

        // Validate minimum cost (half rounded down, minimum 1)
        int cost = declared.getPoints() / 2;
        if (cost < 1) {
            cost = 1;
        }
        if (declared.getPoints() < cost) {
            throw new IllegalStateException(
                "Not enough declared influence to return Politico from Exile.\n"
            );
        }

        // Check if there is an available PEOPLE "ministry" to locate the returned Politico
        List<GameMinistry> availablePeopleMinistries = 
            this.gameMinService.loopPeopleMinistries(game);
        if (availablePeopleMinistries.isEmpty()) {
            throw new IllegalStateException(
                "No vacant People Ministries available for return.\n"
            );
        }
    }

    private void authorizeBeginInvestigation(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Validate if acting GamePolitico and target Politico exist
        GamePolitico actingGamePolitico = action.getActingGamePolitico();
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        if (actingGamePolitico == null || targetGamePolitico == null) {
            throw new IllegalStateException("Acting Minister and target Politico required.\n");
        }
        // Validate if both Politicos belong to this Game
        if (!actingGamePolitico.getGame().equals(game) || 
            !targetGamePolitico.getGame().equals(game)
        ) {
            throw new IllegalStateException(
                "Minister or Target Politico doesn't belong to this game.\n"
            );
        }

        // Validate if acting Politico is controlled by Player
        if (!this.controlPoliticoService.playerControlsPolitico(player, actingGamePolitico)) {
            throw new IllegalStateException("Player doesn't control acting Minister.\n");
        }
        // Validate if acting Politico is ACTIVE
        if (!actingGamePolitico.getStatus().equals(GamePoliticoStatus.ACTIVE)) {
            throw new IllegalStateException(
                "Acting Politico must be ACTIVE to perform an action.\n"
            );
        }

        // Validate if acting Ministry is allowed to perform this action this turn
        GameMinistry actingMinistry = actingGamePolitico.getGameMinistry();
        MinistryEnum actingMinistryName = actingMinistry.getMinistry().getName();
        MinistryEnum currentAuthorized = 
            this.gameMinService.resolveAuthorizedMinistryForActionType(action.getType(), game)
                                    .get(0);
        /* DEBUG */
        System.out.println("Authorized Ministry for " + action.getType().name() + ": " +
                            currentAuthorized.name() + ".\n");
        /********/
        if (!actingMinistryName.equals(currentAuthorized)) {
            throw new IllegalStateException(
                "Acting Ministry is not allowed to perform this action.\n"
            );
        }
        // Validate if acting Politico is assigned to authorized Ministry
        if (!actingMinistry.getMinister().equals(actingGamePolitico)) {
            throw new IllegalStateException(
                "Acting Politico must be assigned to authorized Ministry.\n"
            );
        }

        // Validate if target Politico can be an investigation target.
        MinistryEnum targetMinistryName = targetGamePolitico.getGameMinistry()
                                                            .getMinistry().getName();
        if (targetMinistryName.equals(actingMinistryName) || 
            targetMinistryName.equals(MinistryEnum.PEOPLE)
        ) {
            throw new IllegalStateException("Acting Minister can't investigate him/herself or the People.\n");
        }
        // Validate if target Politico is not in Siberia, on Exile or Inactive (Dead/Not in Game)
        if (targetGamePolitico.getStatus().equals(GamePoliticoStatus.IN_EXILE) ||
            targetGamePolitico.getStatus().equals(GamePoliticoStatus.IN_SIBERIA) ||
            targetGamePolitico.getStatus().equals(GamePoliticoStatus.INACTIVE)
        )
        {
            throw new IllegalStateException("Politicos on Exile, in Siberia or Inactive can't be purged.\n");
        }

        // Validate if target Politico hasn't been investigated this turn
        boolean alreadyInvestigatedThisTurn = 
            this.repoAction.existsByGameAndTypeAndTargetGamePoliticoAndTurn(
                game, 
                ActionType.BEGIN_INVESTIGATION, 
                targetGamePolitico, 
                game.getCurrentTurn()
            );
        if (alreadyInvestigatedThisTurn) {
            throw new IllegalStateException(
                "Target Politico has already been investgated this turn.\n"
            );
        }

        // Validate if target Politico is currently immune to investigations 
        // due to a trial absolution in this turn
        if (targetGamePolitico.getImmuneToInvestigationsUntilTurn() != null) {
            throw new IllegalStateException(
                "Target Politico has been recently absolved on a Trial.\n" + 
                "You may not investigate him/her until next turn.\n"
            );
        }
    }

    private void authorizeRemoveInvestigation(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Validate if acting GamePolitico and target Politico exist
        GamePolitico actingGamePolitico = action.getActingGamePolitico();
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        if (actingGamePolitico == null || targetGamePolitico == null) {
            throw new IllegalStateException("Acting Minister and target Politico required.\n");
        }
        // Validate if both Politicos belong to this Game
        if (!actingGamePolitico.getGame().equals(game) || 
            !targetGamePolitico.getGame().equals(game)
        ) {
            throw new IllegalStateException(
                "Minister or Target Politico doesn't belong to this game.\n"
            );
        }

        // Validate if acting Politico is controlled by Player
        if (!this.controlPoliticoService.playerControlsPolitico(player, actingGamePolitico)) {
            throw new IllegalStateException("Player doesn't control acting Minister.\n");
        }
        // Validate if acting Politico is ACTIVE
        if (!actingGamePolitico.getStatus().equals(GamePoliticoStatus.ACTIVE)) {
            throw new IllegalStateException(
                "Acting Politico must be ACTIVE to perform an action.\n"
            );
        }

        // Validate if acting Ministry is allowed to perform this action this turn
        GameMinistry actingMinistry = actingGamePolitico.getGameMinistry();
        MinistryEnum actingMinistryName = actingMinistry.getMinistry().getName();
        MinistryEnum currentAuthorized = 
            this.gameMinService.resolveAuthorizedMinistryForActionType(action.getType(), game)
                                    .get(0);
        /* DEBUG */
        System.out.println("Authorized Ministry for " + action.getType().name() + ": " +
                            currentAuthorized.name() + ".\n");
        /********/
        if (!actingMinistryName.equals(currentAuthorized)) {
            throw new IllegalStateException(
                "Acting Ministry is not allowed to perform this action.\n"
            );
        }
        // Validate if acting Politico is assigned to authorized Ministry
        if (!actingMinistry.getMinister().equals(actingGamePolitico)) {
            throw new IllegalStateException(
                "Acting Politico must be assigned to authorized Ministry.\n"
            );
        }

        // Validate if target Politico has open investigations
        if (targetGamePolitico.getInvestigationCount() <= 0 )
        {
            throw new IllegalStateException("Target Politico must be under investigation.\n");
        }
    }

    private void authorizeOpenTrial(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Validate if there isn't another awaiting action in progress.
        if (game.getBlockingStatus() != ActionBlockingStatus.NONE ||
            game.getCurrentAwaitingAction() != null
        ) {
            throw new IllegalStateException(
                "Unable to announce action. Awaiting action in progress.\n"
            );
        }

        // Validate if acting GamePolitico and target Politico exist
        GamePolitico actingGamePolitico = action.getActingGamePolitico();
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        if (actingGamePolitico == null || targetGamePolitico == null) {
            throw new IllegalStateException(
                "Acting Minister and target Politico required.\n"
            );
        }
        // Validate if both Politicos belong to this Game
        if (!actingGamePolitico.getGame().equals(game) || 
            !targetGamePolitico.getGame().equals(game)
        ) {
            throw new IllegalStateException(
                "Acting Minister or Target Politico doesn't belong to this game.\n"
            );
        }

        // Validate if acting Politico is controlled by Player
        if (!this.controlPoliticoService.playerControlsPolitico(player, actingGamePolitico)) {
            throw new IllegalStateException("Player doesn't control acting Minister.\n");
        }
        // Validate if acting Politico is ACTIVE
        if (!actingGamePolitico.getStatus().equals(GamePoliticoStatus.ACTIVE)) {
            throw new IllegalStateException(
                "Acting Politico must be ACTIVE to perform an action.\n"
            );
        }

        // Validate if acting Ministry is allowed to perform this action this turn
        GameMinistry actingMinistry = actingGamePolitico.getGameMinistry();
        MinistryEnum actingMinistryName = actingMinistry.getMinistry().getName();
        MinistryEnum currentAuthorized = 
            this.gameMinService.resolveAuthorizedMinistryForActionType(action.getType(), game)
                                    .get(0);
        /* DEBUG */
        System.out.println("Authorized Ministry for " + action.getType().name() + ": " +
                            currentAuthorized.name() + ".\n");
        /********/
        if (!actingMinistryName.equals(currentAuthorized)) {
            throw new IllegalStateException(
                "Acting Ministry is not allowed to perform this action.\n"
            );
        }
        // Validate if acting Politico is assigned to authorized Ministry
        if (!actingMinistry.getMinister().equals(actingGamePolitico)) {
            throw new IllegalStateException(
                "Acting Politico must be assigned to authorized Ministry.\n"
            );
        }

        // Validate if target Politico is not in Siberia, on Exile or Inactive (Dead/Not in Game)
        if (targetGamePolitico.getStatus().equals(GamePoliticoStatus.IN_EXILE) ||
            targetGamePolitico.getStatus().equals(GamePoliticoStatus.IN_SIBERIA) ||
            targetGamePolitico.getStatus().equals(GamePoliticoStatus.INACTIVE)
        )
        {
            throw new IllegalStateException(
                "Politicos in Exile, in Siberia or Inactive can't be brought to Trial.\n"
            );
        }

        // Validate if target Politico had open investigations when this phase began.
        if (targetGamePolitico.getInvestigationCountAtPhaseStart() <= 0) {
            throw new IllegalStateException(
                "Previous investigation required to bring target Politico to Trial.\n"
            );
        }
    }

    private void authorizeCastTrialVote(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Validate action is awaiting votes
        this.trialService.validateTrialAwaitingVotes(game);
        
        // Validate if voter exists
        GamePolitico actingGamePolitico = action.getActingGamePolitico();
        if (actingGamePolitico == null) {
            throw new IllegalStateException("Acting Minister required.\n");
        }
        // Validate if voter belongs to this Game
        if (!actingGamePolitico.getGame().equals(game)) {
            throw new IllegalStateException(
                "Acting Minister or Target Politico doesn't belong to this game.\n"
            );
        }
        // Validate if voter is controlled by Player
        if (!this.controlPoliticoService.playerControlsPolitico(player, actingGamePolitico)) {
            throw new IllegalStateException("Player doesn't control acting Minister.\n");
        }
        // Validate if voter is ACTIVE
        if (!actingGamePolitico.getStatus().equals(GamePoliticoStatus.ACTIVE)) {
            throw new IllegalStateException(
                "Acting Politico must be ACTIVE to perform an action.\n"
            );
        }

        // Validate if acting Ministry is allowed to perform this action this turn
        GameMinistry actingMinistry = actingGamePolitico.getGameMinistry();
        MinistryEnum actingMinistryName = actingMinistry.getMinistry().getName();
        List<MinistryEnum> currentAuthorized = 
            this.gameMinService.resolveAuthorizedMinistryForActionType(action.getType(), game);
        /* DEBUG */
        for (MinistryEnum ministry : currentAuthorized) {
            System.out.println("Authorized Ministries for " + action.getType().name() + ": " +
                                ministry.name() + ".\n");
        }
        /********/
        if (!currentAuthorized.contains(actingMinistryName) || 
            actingMinistryName.equals(MinistryEnum.CANDIDATE) ||
            actingMinistryName.equals(MinistryEnum.PEOPLE))
        {
            throw new IllegalStateException(
                "Acting Ministry is not allowed to perform this action.\n"
            );
        }
        // Validate if acting Politico is assigned to authorized Ministry
        if (!actingMinistry.getMinister().equals(actingGamePolitico)) {
            throw new IllegalStateException(
                "Acting Politico must be assigned to authorized Ministry.\n"
            );
        }

        // Validate if voter is not accused or prosecutor (fixed vote)
        this.trialService.validateAccusedOrProsecutor(game, actingGamePolitico);
        // Validate if voter has already voted
        this.trialService.validateAlreadyVoted(game, actingGamePolitico);
    }

    private void authorizeCondemnation(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        // Validate if there isn't another awaiting action in progress.
        if (game.getBlockingStatus() != ActionBlockingStatus.NONE ||
            game.getCurrentAwaitingAction() != null
        ) {
            throw new IllegalStateException(
                "Unable to announce action. Awaiting action in progress.\n"
            );
        }

        // Validate if acting GamePolitico and target Politico exist
        GamePolitico actingGamePolitico = action.getActingGamePolitico();
        GamePolitico targetGamePolitico = action.getTargetGamePolitico();
        if (actingGamePolitico == null || targetGamePolitico == null) {
            throw new IllegalStateException(
                "Acting Minister and target Politico required.\n"
            );
        }
        // Validate if both Politicos belong to this Game
        if (!actingGamePolitico.getGame().equals(game) || 
            !targetGamePolitico.getGame().equals(game)
        ) {
            throw new IllegalStateException(
                "Acting Minister or Target Politico doesn't belong to this game.\n"
            );
        }

        // Validate if acting Politico is controlled by Player
        if (!this.controlPoliticoService.playerControlsPolitico(player, actingGamePolitico)) {
            throw new IllegalStateException("Player doesn't control acting Minister.\n");
        }
        // Validate if acting Politico is ACTIVE
        if (!actingGamePolitico.getStatus().equals(GamePoliticoStatus.ACTIVE)) {
            throw new IllegalStateException(
                "Acting Politico must be ACTIVE to perform an action.\n"
            );
        }

        // Validate if acting Ministry is allowed to perform this action this turn
        GameMinistry actingMinistry = actingGamePolitico.getGameMinistry();
        MinistryEnum actingMinistryName = actingMinistry.getMinistry().getName();
        MinistryEnum currentAuthorized = 
            this.gameMinService.resolveAuthorizedMinistryForActionType(action.getType(), game)
                                    .get(0);
        /* DEBUG */
        System.out.println("Authorized Ministry for " + action.getType().name() + ": " +
                            currentAuthorized.name() + ".\n");
        /********/
        if (!actingMinistryName.equals(currentAuthorized)) {
            throw new IllegalStateException(
                "Acting Ministry is not allowed to perform this action.\n"
            );
        }
        // Validate if acting Politico is assigned to authorized Ministry
        if (!actingMinistry.getMinister().equals(actingGamePolitico)) {
            throw new IllegalStateException(
                "Acting Politico must be assigned to authorized Ministry.\n"
            );
        }

        // Validate if target Politico is not in Siberia, in Exile or Inactive (Dead/Not in Game)
        if (targetGamePolitico.getStatus().equals(GamePoliticoStatus.IN_EXILE) ||
            targetGamePolitico.getStatus().equals(GamePoliticoStatus.IN_SIBERIA) ||
            targetGamePolitico.getStatus().equals(GamePoliticoStatus.INACTIVE)
        )
        {
            throw new IllegalStateException(
                "Politicos in Exile, in Siberia or Inactive can't be Condemned.\n"
            );
        }
        // Validate target's GameMinistry
        GameMinistry targetMinistry = targetGamePolitico.getGameMinistry();
        if (targetMinistry == null || !targetMinistry.getMinister().equals(targetGamePolitico))
        {
            throw new IllegalStateException(
                "Target Politico must be assigned to a valid Ministry.\n"
            );
        }
        // Validate if target Politico is CANDIDATE
        if (!targetMinistry.getMinistry().getName()
                           .equals(MinistryEnum.CANDIDATE)
        )
        {
            throw new IllegalStateException(
                "Target Politico must be a Candidate.\n"
            );
        }
    }

    private void authorizeNegateCondemnation(
        Player player,
        Game game,
        ActionInstance action
    )
    {
        ActionInstance currentAwaitingAction = game.getCurrentAwaitingAction();
        ActionBlockingStatus blockingStatus = game.getBlockingStatus();

        // Check if there is an awaiting action
        if (blockingStatus.equals(ActionBlockingStatus.NONE) ||
            currentAwaitingAction == null
        ) {
            throw new IllegalStateException(
                "There must be an awaiting action to Negate Condemnation.\n"
            );
        }
        
        ActionType awaitingType = currentAwaitingAction.getType();
        boolean valid = (awaitingType == ActionType.CONDEMNATION &&
                         blockingStatus == ActionBlockingStatus.AWAITING_CONDEMNATION_RESPONSE);
        
        // Check if awaiting action is a CONDEMNATION
        if ( !valid ) {
            throw new IllegalStateException(
                "Negate Condemnation can only be announced immediately after a Condemnation announcement.\n");
        }
        // Check awaiting action's status (must be ANNOUNCED)
        if (!currentAwaitingAction.getStatus().equals(ActionStatus.ANNOUNCED)) {
            throw new IllegalStateException(
                "Invalid awaiting action status.\n"
            );
        }
        
        GamePolitico negateCondemnationTarget = action.getTargetGamePolitico();
        GamePolitico awaitingActionTarget = currentAwaitingAction.getTargetGamePolitico();
        
        // Check if awaiting action's target is the same as Negate Condemnation's target
        if (!awaitingActionTarget.equals(negateCondemnationTarget)) {
            throw new IllegalStateException(
                "Negate Condemnation's target Politico and awaiting Condemnation's target must be the same.\n"
            );
        }

        // Check if acting Politico is the PARTY_CHIEF or a 1st level Minister
        GamePolitico actingMinister = action.getActingGamePolitico();
        MinistryEnum actingMinistryName = actingMinister.getGameMinistry()
                                                        .getMinistry()
                                                        .getName();
        List<MinistryEnum> allowedMinistries = 
            GameMinistryService.NEGATE_CONDEMNATION_PROMOTERS;

        if (!allowedMinistries.contains(actingMinistryName)) {
            throw new IllegalStateException(
                "Negating Minister must be th Party Chief or a 1st level Minister.\n"
            );
        }

        // Check if acting Politico is ACTIVE
        if (!actingMinister.getStatus().equals(GamePoliticoStatus.ACTIVE)) {
            throw new IllegalStateException(
                "Acting Minister must be Active.\n"
            );
        }

        // Check if acting Minister is not the same who announced the awaiting Condemnation
        if (actingMinister.equals(currentAwaitingAction.getActingGamePolitico())) {
            throw new IllegalStateException(
                "Negating Minister cannot be the same who announced the Condemnation.\n"
            );
        }

        // Check if Player controls acting Minister
        if (!this.controlPoliticoService.playerControlsPolitico(player, actingMinister)) {
            throw new IllegalStateException(
                "Player must control the promoter of a Negation.\n"
            );
        }
    }
    
}
