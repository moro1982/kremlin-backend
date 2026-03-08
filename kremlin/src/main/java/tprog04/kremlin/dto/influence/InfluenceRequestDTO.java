package tprog04.kremlin.dto.influence;

import lombok.Data;

@Data
public abstract class InfluenceRequestDTO {
    private Integer points;
    private Long playerId;
    private Long gamePoliticoId;
}
