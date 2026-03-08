package tprog04.kremlin.dto.player;

import java.util.Map;
import java.util.Set;
import lombok.Data;
import tprog04.kremlin.aux_classes.Faction;

@Data
public class PlayerResponseDTO {
    private Long id;
    private String name;
    private Faction faction;
    private Long userID;
    private Long gameID;
    private boolean ready;
    private Map<Long, Integer> assignedInfluences;  // { gamePolID -> influencePoints}
    private Map<Long, Integer> declaredInfluences;  // { gamePolID -> influencePoints}
    private Set<Long> controlledPoliticosIDs;
}
