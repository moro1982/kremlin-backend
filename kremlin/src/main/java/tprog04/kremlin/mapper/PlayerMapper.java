package tprog04.kremlin.mapper;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tprog04.kremlin.dto.player.LobbyPlayerDTO;
import tprog04.kremlin.dto.player.PlayerRequestDTO;
import tprog04.kremlin.dto.player.PlayerResponseDTO;
import tprog04.kremlin.models.InfluenceAssigned;
import tprog04.kremlin.models.InfluenceDeclared;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.InfluenceAssignedRepository;
import tprog04.kremlin.repositories.InfluenceDeclaredRepository;
import tprog04.kremlin.services.ControlPoliticoService;

@Component
public class PlayerMapper {
    
    @Autowired
    private GameRepository repoGame;
    @Autowired
    private InfluenceAssignedRepository repoAssigned;
    @Autowired
    private InfluenceDeclaredRepository repoDeclared;
    @Autowired
    private ControlPoliticoService controlService;

    public PlayerResponseDTO toDto(Player player, Long myPlayerID) {
        PlayerResponseDTO dto = new PlayerResponseDTO();
        dto.setId(player.getId());
        dto.setName(player.getName());
        dto.setFaction(player.getFaction());
        dto.setUserID(player.getUser().getId());
        dto.setGameID(player.getGame().getId());
        dto.setReady(player.getGame().getReadyPlayers().contains(player.getId()));
        
        // PUBLIC
        if (!player.getDeclared().isEmpty()) {
            dto.setDeclaredInfluences(
                player.getDeclared().stream()
                      .filter(Objects::nonNull)
                      .filter(di -> di.getGamePolitico() != null)
                      .filter(di -> di.getGamePolitico().getId() != null)
                      .filter(di -> di.getPoints() != null)
                      .collect(Collectors.toMap(
                        di -> di.getGamePolitico().getId(),
                        InfluenceDeclared::getPoints
                      ))
            );
        } else {
            dto.setDeclaredInfluences(Collections.emptyMap());
        }

        if (!this.controlService.getPoliticosControlledByPlayer(player.getId()).isEmpty()) {
            dto.setControlledPoliticosIDs(
                this.controlService.getPoliticosControlledByPlayer(player.getId())
                                   .stream()
                                   .map(gp -> gp.getId())
                                   .collect(Collectors.toSet())
            );
        } else {
            dto.setControlledPoliticosIDs(Collections.emptySet());
        }

        // PRIVATE (only if it's myPlayer)
        if (player.getId().equals(myPlayerID)) {
            // If Player has assigned any influence
            if (!player.getAssigned().isEmpty()) {
                dto.setAssignedInfluences(
                    player.getAssigned().stream()
                          .filter(Objects::nonNull)
                          .filter(ai -> ai.getGamePolitico() != null)
                          .filter(ai -> ai.getGamePolitico().getId() != null)
                          .filter(ai -> ai.getPoints() != null)
                          .collect(Collectors.toMap(
                            ai -> ai.getGamePolitico().getId(),
                            InfluenceAssigned::getPoints)
                          )
                );
            }
        } else {
            dto.setAssignedInfluences(Collections.emptyMap());
        }
        
        return dto;
    }

    public LobbyPlayerDTO toLobbyPlayerDTO(Player player) {
        
        LobbyPlayerDTO dto = new LobbyPlayerDTO();
        dto.setPlayerID(player.getId());
        dto.setUserID(player.getUser().getId());
        dto.setName(player.getName());
        dto.setFaction(
            player.getFaction() != null
                        ? player.getFaction()
                        : null
        );
        dto.setReady(
            player.getGame()
                  .getReadyPlayers()
                  .contains(player.getId())
        );

        return dto;
    }

    public Player toEntity(PlayerRequestDTO dto) {
        Player player = new Player();
        player.setName(dto.getName());
        player.setFaction(dto.getFaction());
        player.setGame(this.repoGame.findById(dto.getGameID()).get());
        if (dto.getAssignedIds() != null) {
            player.setAssigned(
                this.repoAssigned.findAllById(dto.getAssignedIds())
                                 .stream()
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toSet())
            );
        }
        if (dto.getDeclaredIds() != null) {
            player.setDeclared(
                this.repoDeclared.findAllById(dto.getDeclaredIds())
                                 .stream()
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toSet())
            );
        }
        return player;
    }
}
