package tprog04.kremlin.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tprog04.kremlin.aux_classes.ActionBlockingStatus;
import tprog04.kremlin.aux_classes.GameStatus;
import tprog04.kremlin.aux_classes.PhaseExecutionStatus;

@Entity
@Getter
@Setter
@ToString(exclude = {"players", "readyPlayers", "gamePoliticos"})
@EqualsAndHashCode(exclude = {"players", "readyPlayers", "gamePoliticos"})
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private Integer currentTurn = 0;    // 0 : Game not begun - 1 to 10 only
    private Integer currentPhase = 0;   // 0 : Game not begun - 1 to 8 only
    private boolean finished = false;

    // Optimistic concurrency control
    @Version
    private Long version;
    // Logical counter for polling sync from frontend.
    private Long updateCounter = 0L;

    // Maximum number of players for this Game (set when created)
    private Integer maxPlayers;

    // Game status (OPEN, CLOSED, IN_PROGRESS, PAUSED, CANCELLED)
    @Enumerated(EnumType.STRING)
    private GameStatus status;

    // Phase execution status
    @Enumerated(EnumType.STRING)
    private PhaseExecutionStatus phaseStatus;


    /* Main relationships */

    @OneToMany(mappedBy = "game")
    private Set<Player> players = new HashSet<>();
    
    @OneToMany(mappedBy = "game")
    private Set<GamePolitico> gamePoliticos = new HashSet<>();

    // Players that already marked "ready"
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "game_ready_players",
        joinColumns = @JoinColumn(name = "game_id")
    )
    @Column(name = "player_id")
    private Set<Long> readyPlayers = new HashSet<>();


    /* Specific fields */
    /*
    * Action that blocks game's advance until it's resolved.
    * (PURGE_ATTEMPT, OPEN_TRIAL, etc.)
    */
    @OneToOne(optional = true)
    @JoinColumn(name = "current_awaiting_action_id")
    private ActionInstance currentAwaitingAction;

    @Enumerated(EnumType.STRING)
    private ActionBlockingStatus blockingStatus;

    @OneToOne(optional = true)
    private Trial trial;


    /* Methods */

    public void markPlayerReady(Player player) {
        if (readyPlayers == null) {
            readyPlayers = new HashSet<>();
        }
        readyPlayers.add(player.getId());
    }

    public boolean allPlayersReady() {
        return readyPlayers != null && 
               readyPlayers.size() == players.size();
    }

    public void clearReadyPlayers() {
        if (readyPlayers != null) {
            readyPlayers.clear();
        }
    }

    public boolean hasPlayer(User user) {
        return this.players.stream().anyMatch(p -> p.getUser().equals(user));
    }

    // Increment logical sync counter
    public void notifyUpdate() {
        this.updateCounter++;
    }

}
