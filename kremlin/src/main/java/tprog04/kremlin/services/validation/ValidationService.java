package tprog04.kremlin.services.validation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import tprog04.kremlin.aux_classes.ActionStatus;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.dto.influence.assigned.AssignedResponseDTO;
import tprog04.kremlin.models.ActionInstance;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.models.User;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.PlayerRepository;
import tprog04.kremlin.repositories.UserRepository;

@Component
public class ValidationService {

    @Autowired
    private UserRepository repoUser;
    @Autowired
    private GameRepository repoGame;
    @Autowired
    private PlayerRepository repoPlayer;

    // Validate if Game exists
    public Game validateGameByID(Long gameID) {
        Game found = this.repoGame.findById(gameID).orElseThrow(
            () -> new IllegalStateException(
                "Game not found.\n"
            )
        );
        return found;
    }
    
    // Get current User from security context (JWT token)
    public User getCurrentUser() {
        
        // Bring auth token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Validate if authenticated
        if (auth == null || 
            !auth.isAuthenticated() ||
            auth instanceof AnonymousAuthenticationToken
        ) {
            throw new RuntimeException("Unauthenticated user.");
        }

        // Extract principal
        Object principal = auth.getPrincipal();
        String username;

        if (principal instanceof UserDetails userDetails) {
            // Extract user details
            username = userDetails.getUsername();
        } else if (principal instanceof String principalStr) {
            // JWT / SSE / anonymous
            if ("anonymousUser".equals(principalStr)) {
                throw new RuntimeException("Anonymous User.");
            }
            username = principalStr;
        } else {
            throw new RuntimeException(
                "Unsupported Principal type: " + principal.getClass()
            );
        }
        
        // Find User by username
        User user = this.repoUser.findByUsername(username)
                                 .orElseThrow(
                                    () -> new RuntimeException("User Not Found.\n")
                                 );
        return user;
    }

    public Player validatePlayerByID(Long playerID) {
        Player found = this.repoPlayer.findById(playerID)
                                      .orElseThrow(
                                        () -> new IllegalStateException(
                                            "Player not found.\n"
                                        )
                                      );
        return found;
    }

    public Player getPlayerByUserAndGame(User user, Game game) {
        // Find Player by User and Game
        Player player = this.repoPlayer.findByUserAndGame(user, game)
                                       .orElseThrow(
                                          () -> new RuntimeException("Player not found for this User and Game.\n")
                                       );
        return player;
    }

    // Validate if ActionInstance belongs to a certain Game
    public void validateActionBelongsToGame(ActionInstance action, Game game) {
        if (!action.getGame().equals(game)) {
            throw new IllegalStateException("Action doesn't belong to this Game.\n");
        }
    }

    // Validate if ActionInstance was announced by Player
    public void validateActionOwner(ActionInstance action, Player actor) {
        if ( !action.getActor().equals(actor) ) {
            throw new IllegalStateException("Player doesn't own this Action.\n");
        }
    }

    // Validate if ActionInstance's state is ANNOUNCED
    public void validateActionIsAnnounced(ActionInstance action) {
        if ( action.getStatus() != ActionStatus.ANNOUNCED ) {
            throw new IllegalStateException("Only ANNOUNCED actions can be cancelled");
        }
    }

    // Validate Action is allowed in current Phase
    public void validateActionAllowedInPhase(ActionType type, Game game) {
        Set<ActionType> allowedActions = ActionType.fromOrder(game.getCurrentPhase());
        if (!allowedActions.isEmpty() && !allowedActions.contains(type)) {
            throw new IllegalStateException("Invalid Phase for this Action.\n");
        }
    }

    // Validate Game has awaiting action
    public void validateGameHasAwaitingAction(Game game) {
        ActionInstance awaitingAction = game.getCurrentAwaitingAction();
        if (awaitingAction == null) {
            throw new IllegalStateException("No action awaiting resolution.\n");
        }
    }

    // Validate if a list of Assigned Influences contains 10 unrepeated (unique) values
    public void validatePlayerAssignedInfluences( List<AssignedResponseDTO> influences) {
        
        if (influences.size() != 10) {
            throw new IllegalStateException(
                "Player must assign exactly 10 influences."
            );
        }

        Set<Integer> values = influences.stream()
                                        .map(dto -> dto.getPoints())
                                        .collect(Collectors.toSet());
        if (values.size() != 10) {
            throw new IllegalStateException(
                "Player must assign exactly 10 influences."
            );
        }
        // Check unique values from 1 to 10
        for (int i = 1; i <= 10; i++) {
            if (!values.contains(i)) {
                throw new IllegalStateException("Missing influence value: " + i);
            }
        }
    }

}
