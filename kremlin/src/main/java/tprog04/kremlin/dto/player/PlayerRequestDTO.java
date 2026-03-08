package tprog04.kremlin.dto.player;

import java.util.Set;
import lombok.Data;
import tprog04.kremlin.aux_classes.Faction;

@Data
public class PlayerRequestDTO {
    private String name;
    private Faction faction;
    private Long gameID;
    private Set<Long> assignedIds;
    private Set<Long> declaredIds;
}
