package tprog04.kremlin.controllers;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tprog04.kremlin.dto.control.GameMinistryStatusDTO;
import tprog04.kremlin.dto.gamePolitico.GamePoliticoResponseDTO;
import tprog04.kremlin.dto.influence.declared.DeclaredResponseDTO;
import tprog04.kremlin.mapper.GamePoliticoMapper;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.services.ControlPoliticoService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/control")
public class ControlPoliticoController {

    @Autowired
    private ControlPoliticoService controlService;
    @Autowired
    private GamePoliticoMapper gamePolMapper;
    
    /* Get Player controlling a Politico */
    @GetMapping("/politico/{id}/controller")
    public Player getControllingPlayer(@PathVariable("id") Long politicoID) {
        Player controller = this.controlService.getControllingPlayer(politicoID);
        return controller;
    }

    /* Get Influence declarations on a Politico */
    @GetMapping("/politico/{id}/declarations")
    public List<DeclaredResponseDTO> getInfluenceDeclarations(@PathVariable("id") Long politicoID) {
        return controlService.getInfluenceDeclarationsOnPolitico(politicoID);
    }

    /* Get Politicos controlled by Player */
    @GetMapping("/player/{id}/politicos")
    public List<GamePoliticoResponseDTO> getControlledPoliticos(@PathVariable("id") Long playerID) {
        List<GamePolitico> entities = controlService.getPoliticosControlledByPlayer(playerID);
        return entities.stream()
                       .map(gamePolMapper::toDto)
                       .collect(Collectors.toList());
    }

    /* Get list of MinistryStatus (Ministry, Minister and Controller) */
    @GetMapping("/ministry/status")
    public List<GameMinistryStatusDTO> getAllMinistryStatus() {
        return controlService.getAllMinistryStatus();
    }
}
