package tprog04.kremlin.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tprog04.kremlin.dto.game.GameContextDTO;
import tprog04.kremlin.dto.game.GameResponseDTO;
import tprog04.kremlin.dto.game.GameSummaryDTO;
import tprog04.kremlin.services.game.GameService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/game/base")
public class GameController {
    
    @Autowired
    private GameService gameService;

    /**
     * GAME STATE
    **/
    @GetMapping("/find/{game_id}")
    public ResponseEntity<GameResponseDTO> getGameByID
        (@PathVariable("game_id") Long game_id)
    {
        return ResponseEntity.ok(this.gameService.getGameById(game_id));
    }

    @GetMapping("/find_by_user")
    public List<GameResponseDTO> getGamesByCurrentUser() {
        return this.gameService.getGamesByCurrentUser();
    }

    @GetMapping("/summary_list")
    public List<GameSummaryDTO> getGameSummaries() {
        return this.gameService.getGameSummaries();
    }
    
    @GetMapping("/context/{game_id}")
    public GameContextDTO getGameContext(
        @PathVariable("game_id") Long game_id
    )
    {
        return this.gameService.getGameContext(game_id);
    }

    @GetMapping("/state/{game_id}")
    public ResponseEntity<?> getGameState
        ( 
            @PathVariable("game_id") Long game_id,
            @RequestParam(required = false) Long version
        )
    {
        GameResponseDTO gameDTO = this.gameService.getGameState(game_id);
        if (version != null && version >= gameDTO.getVersion()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(gameDTO);
    }

    @PostMapping("/new/max_players/{maxPlayers}")
    public ResponseEntity<GameResponseDTO> createNewGame(
        @PathVariable("maxPlayers") int maxPlayers
    ) {
        return ResponseEntity.ok(this.gameService.createNewGame(maxPlayers));
    }
    
    @PostMapping("/{game_id}/lobby/toggle-ready")
    public ResponseEntity<Void> lobbyToggleReady(@PathVariable("game_id") Long game_id) {
        this.gameService.lobbyToggleReady(game_id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{game_id}/begin-influence-assignment")
    public ResponseEntity<Void> beginInfluenceAssignment(
        @PathVariable("game_id") Long game_id
    )
    {
        this.gameService.beginInfluenceAssignment(game_id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{game_id}/confirm-influence-assignment")
    public ResponseEntity<Void> confirmInfluenceAssignment(
        @PathVariable("game_id") Long game_id
    )
    {
        this.gameService.confirmInfluenceAssignment(game_id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/start/{game_id}")
    public ResponseEntity<GameResponseDTO> startGame(@PathVariable("game_id") Long game_id)
    {
        return ResponseEntity.ok(this.gameService.startGame(game_id));
    }

    
    /**
     * PHASE MANAGEMENT
    **/
    @PostMapping("/next_phase/{game_id}")
    public ResponseEntity<GameResponseDTO> nextPhase(@PathVariable("game_id") Long game_id) {
        return ResponseEntity.ok(this.gameService.nextPhase(game_id));
    }

    @PostMapping("/begin_phase/{game_id}")
    public ResponseEntity<GameResponseDTO> beginCurrentPhase
        (@PathVariable("game_id") Long game_id)
    {
        return ResponseEntity.ok(this.gameService.beginCurrentPhase(game_id));
    }

    @PostMapping("/confirm_phase_exec/{game_id}")
    public ResponseEntity<GameResponseDTO> confirmPhaseExecution
        (@PathVariable("game_id") Long game_id)
    {
        return ResponseEntity.ok(this.gameService.confirmPhaseExecution(game_id));
    }

    @PostMapping("/resolve_awaiting/{game_id}")
    public ResponseEntity<GameResponseDTO> resolveAwaitingAction
        (@PathVariable("game_id") Long game_id)
    {
        return ResponseEntity.ok(this.gameService.resolveAwaitingAction(game_id));
    }

    @PostMapping("/end_phase/{game_id}")
    public ResponseEntity<GameResponseDTO> endCurrentPhase
        (@PathVariable("game_id") Long game_id)
    {
        return ResponseEntity.ok(this.gameService.endCurrentPhase(game_id));
    }

    @PostMapping("/{game_id}/playerReady")
    public ResponseEntity<?> markPlayerReady( @PathVariable("game_id") Long game_id )
    {
        GameResponseDTO dto = this.gameService.markPlayerReady(game_id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }


    /**
     * TRIAL (endpoints)
    **/
    public GameResponseDTO beginTrial(Long gameID) {
        return this.gameService.beginTrial(gameID);
    }

    public GameResponseDTO readyAfterTrialVote(Long gameID) {
        return this.gameService.readyAfterTrialVote(gameID);
    }

    public GameResponseDTO resumeTrialVoting(Long gameID) {
        return this.gameService.resumeTrialVoting(gameID);
    }


    /**
     * HELPERS (only during development)
    **/

    @GetMapping("/find/all")
    public List<GameResponseDTO> getAllGames() {
        return this.gameService.getAllGames();
    }

    @PostMapping("/resetReady/{game_id}")
    public ResponseEntity<Void> resetReady(@PathVariable("game_id") Long game_id) {
        this.gameService.clearReadyPlayers(game_id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/playersReady/{game_id}")
    public ResponseEntity<Boolean> allPlayersReady(@PathVariable("game_id") Long game_id)
    {
        return ResponseEntity.ok(this.gameService.allPlayersReady(game_id));
    }

    @GetMapping("/updates/{game_id}")
    public ResponseEntity<Long> getUpdateCounter(@PathVariable("game_id") Long game_id)
    {
        return ResponseEntity.ok(this.gameService.getUpdateCounter(game_id));
    }

}
