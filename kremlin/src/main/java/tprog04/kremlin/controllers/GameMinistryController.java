package tprog04.kremlin.controllers;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tprog04.kremlin.dto.gameMinistry.GameMinistryRequestDTO;
import tprog04.kremlin.dto.gameMinistry.GameMinistryResponseDTO;
import tprog04.kremlin.mapper.GameMinistryMapper;
import tprog04.kremlin.models.GameMinistry;
import tprog04.kremlin.services.GameMinistryService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/game/ministry")
public class GameMinistryController {

    @Autowired
    private GameMinistryService gameMinService;
    @Autowired
    private GameMinistryMapper gameMinMapper;

    @GetMapping("/find/{id}")
    public GameMinistryResponseDTO getGameMinistryByID(@PathVariable("id") Long id) {
        GameMinistry found = this.gameMinService.getGameMinistryByID(id);
        return this.gameMinMapper.toDto(found);
    }

    @GetMapping("/in_game/{game_id}")
    public List<GameMinistryResponseDTO> getGameMinistriesByGame
        (@PathVariable("game_id") Long game_id)
    {
        List<GameMinistry> ministries = this.gameMinService.getGameMinistriesByGame(game_id);
        return ministries.stream()
                         .map(gameMinMapper::toDto)
                         .collect(Collectors.toList());
    }

    @PostMapping("/single")
    public GameMinistryResponseDTO createSingleGameMinistry
        (@RequestBody GameMinistryRequestDTO dto)
    {
        GameMinistry created = this.gameMinService.createSingleGameMinistry(dto);
        return this.gameMinMapper.toDto(created);
    }

    @PostMapping("/many")
    public List<GameMinistryResponseDTO> createManyGameMinistries
        (@RequestBody List<GameMinistryRequestDTO> dtos)
    {
        List<GameMinistry> created = this.gameMinService.createManyGameMinistries(dtos);
        return created.stream()
                      .map(gameMinMapper::toDto)
                      .collect(Collectors.toList());
    }

    @PostMapping("/assign/{game_min}/minister/{game_pol}")
    public GameMinistryResponseDTO assignMinister
        (@PathVariable("game_min") Long game_min, 
         @PathVariable("game_pol") Long game_pol)
    {
        return this.gameMinService.assignMinister(game_min, game_pol);
    }

    @PostMapping("/remove/minister/{game_min}")
    public GameMinistryResponseDTO removeMinister(@PathVariable("game_min") Long game_min)
    {
        return this.gameMinService.removeMinister(game_min);
    }

    @PostMapping("/loadAll/{game_id}")
    public List<GameMinistryResponseDTO> loadGameMinistries(@PathVariable("game_id") Long game_id) {
        List<GameMinistry> loaded = this.gameMinService.loadGameMinistries(game_id);
        return loaded.stream()
                     .map(gameMinMapper::toDto)
                     .collect(Collectors.toList());
    }

    @PostMapping("/assign/{game_id}/initial_ministers")
    public List<GameMinistryResponseDTO> assignInitialMinisters
        (@PathVariable("game_id") Long game_id)
    {
        List<GameMinistry> assigned = this.gameMinService.assignInitialMinisters(game_id);
        return assigned.stream()
                       .map(gameMinMapper::toDto)
                       .collect(Collectors.toList());
    }
}
