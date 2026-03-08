package tprog04.kremlin.dto.player;

import lombok.Data;
import tprog04.kremlin.aux_classes.Faction;

@Data
public class LobbyPlayerDTO {
    private Long playerID;
    private Long userID;
    private String name;
    private Faction faction;
    private boolean ready;
}
