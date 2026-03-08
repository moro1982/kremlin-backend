package tprog04.kremlin.models;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tprog04.kremlin.aux_classes.ActionStatus;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.aux_classes.TrialVoteValue;

@Data
@EqualsAndHashCode(exclude = {"game", "actor"})
@Entity
public class ActionInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player actor;

    @Enumerated(EnumType.STRING)
    private ActionType type; // DECLARE_INFLUENCE, PURGE_ATTEMPT, ETC

    @Enumerated(EnumType.STRING)
    private ActionStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private Integer turn;

    private Integer phase;

    // Priority is assigned according to ActionType
    private Integer priority; // lower value -> higher priority

    private boolean resolved = false;

    @ManyToOne
    @JoinColumn(name = "target_id")
    private GamePolitico targetGamePolitico;

    // For Influence Declaration
    private Integer influencePoints;

    // For Trials
    @ManyToOne
    private Trial trial;
    
    // For CAST_TRIAL_VOTE
    private TrialVoteValue trialVoteValue;

    // For Promotion/Demotion/Switch of Politicians
    @ManyToOne
    @JoinColumn(name = "target_ministry_id")
    private GameMinistry targetGameMinistry;

    // For actions performed by certain ministers
    @ManyToOne
    @JoinColumn(name = "acting_game_politico_id")
    private GamePolitico actingGamePolitico;

}
