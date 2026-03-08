package tprog04.kremlin.dto.game;

import java.time.LocalDateTime;
import lombok.Data;
import tprog04.kremlin.aux_classes.GameLifeCycleStatus;

@Data
public class GameSummaryDTO {

    private Long id;

    // Logical Game status
    private GameLifeCycleStatus lifeCycleStatus;

    // Players
    private Integer playerCount;
    private Integer maxPlayers;

    // Relation to authenticated User
    private boolean iAmParticipant;
    private boolean joinable;
    private boolean resumable;

    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;

    public GameSummaryDTO(
        Long id, 
        GameLifeCycleStatus lifeCycleStatus,
        Integer playerCount, 
        Integer maxPlayers,
        boolean iAmParticipant,
        boolean joinable,
        boolean resumable,
        LocalDateTime createdAt,
        LocalDateTime startedAt
    )
    {
        this.id = id;
        this.lifeCycleStatus = lifeCycleStatus;
        this.playerCount = playerCount;
        this.maxPlayers = maxPlayers;
        this.iAmParticipant = iAmParticipant;
        this.joinable = joinable;
        this.resumable = resumable;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
    }
}
