package tprog04.kremlin.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tprog04.kremlin.dto.actionInstance.ActionInstanceDTO;
import tprog04.kremlin.services.game.ActionService;

@RestController
@RequestMapping("/action")
public class ActionController {

    @Autowired
    private ActionService actionService;

    // Registrar una acción (la crea y la deja pendiente)
    @PostMapping("/announce")
    public ResponseEntity<?> announceAction( @RequestBody ActionInstanceDTO dto )
    {
        ActionInstanceDTO created = this.actionService.announceAction(dto);
        return ResponseEntity.ok(created);
    }

    // Cancelar una acción (cambia su ActionStatus a CANCELLED)
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelAction( @RequestBody ActionInstanceDTO dto )
    {
        ActionInstanceDTO cancelled = this.actionService.cancelAction(dto);
        return ResponseEntity.ok(cancelled);
    }

    // Ejecutar (resolver) todas las acciones pendientes de la partida
    @PostMapping("/resolve/{gameId}")
    public ResponseEntity<Void> resolvePendingActions(@PathVariable Long gameId) {
        this.actionService.resolvePendingActions(gameId);
        return ResponseEntity.ok().build();
    }

    // Consultar todas las acciones pendientes
    @GetMapping("/pending/{gameId}")
    public ResponseEntity<List<ActionInstanceDTO>> getPendingActions(@PathVariable Long gameId) {
        return ResponseEntity.ok(actionService.getPendingActions(gameId));
    }

    // Consultar acciones por jugador
    @GetMapping("/game/{game_id}/player/{player_id}")
    public ResponseEntity<List<ActionInstanceDTO>> getActionsByPlayer
        (@PathVariable("game_id") Long game_id, @PathVariable("player_id") Long player_id)
    {
        return ResponseEntity.ok(actionService.getActionsByPlayer(game_id, player_id));
    }

    
}
