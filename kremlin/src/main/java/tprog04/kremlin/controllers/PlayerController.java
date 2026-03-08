package tprog04.kremlin.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tprog04.kremlin.aux_classes.Faction;
import tprog04.kremlin.dto.player.PlayerRequestDTO;
import tprog04.kremlin.dto.player.PlayerResponseDTO;
import tprog04.kremlin.services.PlayerService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/player")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    @GetMapping("/{playerID}/game/{gameID}")
    public PlayerResponseDTO getPlayerById(
        @PathVariable("playerID") Long playerID,
        @PathVariable("gameID") Long gameID
    ) {
        return this.playerService.getPlayerByID(gameID, playerID);
    }

    @GetMapping("/game/{gameID}/all")
    public List<PlayerResponseDTO> getAllPlayers(@PathVariable("gameID") Long gameID) {
        return this.playerService.getAllPlayers(gameID);
    }

    @GetMapping("/game/{gameID}")
    public List<PlayerResponseDTO> getAllPlayersByGame(@PathVariable("gameID") Long gameID)
    {
        return this.playerService.getPlayersByGameId(gameID);
    }

    @GetMapping("/factions")
    public List<Faction> getAllFactions() {
        return this.playerService.getAllFactions();
    }

    @PostMapping("/game/{gameID}/single")
    public PlayerResponseDTO createSinglePlayer(
        @PathVariable("gameID") Long gameID,
        @RequestBody PlayerRequestDTO dto
    )
    {
        return this.playerService.createSinglePlayer(dto);
    }
}
