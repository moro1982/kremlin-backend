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
import tprog04.kremlin.dto.gamePolitico.GamePoliticoRequestDTO;
import tprog04.kremlin.dto.gamePolitico.GamePoliticoResponseDTO;
import tprog04.kremlin.mapper.GamePoliticoMapper;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.services.GamePoliticoService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/game/politico")
public class GamePoliticoController {

    @Autowired
    private GamePoliticoService gamePolService;
    @Autowired
    private GamePoliticoMapper gamePolMapper;

    @GetMapping("/find/{id}")
    public GamePoliticoResponseDTO getGamePoliticoByID(@PathVariable("id") Long id) {
        return this.gamePolService.getGamePoliticoByID(id);
    }

    @GetMapping("/in_game/{id}")
    public List<GamePoliticoResponseDTO> getPoliticosByGameID(@PathVariable("id") Long id)
    {
        List<GamePolitico> gamePoliticos = this.gamePolService.getPoliticosByGameID(id);
        return gamePoliticos.stream()
                            .map(gamePolMapper::toDto)
                            .collect(Collectors.toList());
    }

    @PostMapping("/single")
    public GamePoliticoResponseDTO createSingleGamePolitico
        (@RequestBody GamePoliticoRequestDTO dto)
    {
        GamePolitico created = this.gamePolService.createSingleGamePolitico(dto);
        return gamePolMapper.toDto(created);
    }

    @PostMapping("/many")
    public List<GamePoliticoResponseDTO> createManyGamePoliticos
        (@RequestBody List<GamePoliticoRequestDTO> dtos)
    {
        List<GamePolitico> created = this.gamePolService.createManyGamePoliticos(dtos);
        return created.stream()
                      .map(gamePolMapper::toDto)
                      .collect(Collectors.toList());
    }

    @PostMapping("/loadAll/{game_id}")
    public List<GamePoliticoResponseDTO> loadGamePoliticos(@PathVariable("game_id") Long game_id)
    {
        List<GamePolitico> loaded = this.gamePolService.loadGamePoliticos(game_id);
        return loaded.stream()
                     .map(gamePolMapper::toDto)
                     .collect(Collectors.toList());
    }

}
