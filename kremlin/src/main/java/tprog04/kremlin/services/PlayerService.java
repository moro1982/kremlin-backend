package tprog04.kremlin.services;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tprog04.kremlin.aux_classes.Faction;
import tprog04.kremlin.dto.player.PlayerRequestDTO;
import tprog04.kremlin.dto.player.PlayerResponseDTO;
import tprog04.kremlin.mapper.PlayerMapper;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.models.User;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.PlayerRepository;
import tprog04.kremlin.services.validation.ValidationService;

@Service
public class PlayerService {

    @Autowired
    private PlayerMapper playerMapper;
    @Autowired
    private PlayerRepository repoPlayer;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private GameRepository repoGame;

    public PlayerResponseDTO getPlayerByID(Long gameID, Long playerID) {
        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);
        Player found = repoPlayer.findById(playerID).get();
        return this.playerMapper.toDto(found, myPlayer.getId());
    }

    public List<PlayerResponseDTO> getAllPlayers(Long gameID) {
        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);
        return repoPlayer.findAll()
                         .stream()
                         .map( p -> this.playerMapper.toDto(p, myPlayer.getId()) )
                         .collect(Collectors.toList());
    }

    public List<PlayerResponseDTO> getPlayersByGameId(Long gameID) {
        Game game = this.validationService.validateGameByID(gameID);
        User currentUser = this.validationService.getCurrentUser();
        Player myPlayer = this.validationService.getPlayerByUserAndGame(currentUser, game);
        List<Player> players = this.repoPlayer.findPlayersByGameId(gameID);
        return players.stream()
                      .map( p -> this.playerMapper.toDto(p, myPlayer.getId()) )
                      .toList();
    }

    public List<Player> getPlayersByUser(User user) {
        List<Player> players = this.repoPlayer.findPlayersByUser(user);
        return players;
    }

    public PlayerResponseDTO createSinglePlayer(PlayerRequestDTO dto) {
        // Get current User from Security Context
        User user = this.validationService.getCurrentUser();
        // Get Game by DTO's game ID
        Game game = this.repoGame.findById(dto.getGameID())
                                 .orElseThrow(
                                    () -> new RuntimeException("Game not found")
                                 );
        // Check if Player already exists for this User and Game 
        Player actor = this.repoPlayer.findByUserAndGame(user, game).orElse(null);
        if (actor != null) {
            return this.playerMapper.toDto(actor, actor.getId());
        }
        // If Player doesn't exist, create new Player
        Player newPlayer = new Player();
        // Si el número de Players es menor al máximo permitido
        Set<Player> currentPlayers = game.getPlayers();
        if (currentPlayers.size() < 6) {
            // Recojo lista de Factions ya seleccionadas
            List<Faction> currentFactions = currentPlayers.stream()
                                                          .map(Player::getFaction)
                                                          .collect(Collectors.toList());
            // Si la Faction del DTO está disponible
            if (!currentFactions.contains(dto.getFaction())) {
                // Creo nuevo Player
                newPlayer = playerMapper.toEntity(dto);
                newPlayer.setUser(user);       // Seteo Player::user con el User obtenido.
                this.repoPlayer.save(newPlayer);
                game.getPlayers().add(newPlayer);
            } else {
                System.out.println("La Facción elegida no está disponible.\n");
            }
        } else {
            System.out.println("Esta Partida ha alcanzado el máximo de Jugadores.\n");
        }
        
        return playerMapper.toDto(newPlayer, newPlayer.getId());
    }

    public List<Faction> getAllFactions() {
        Faction[] factionsArray = Faction.values();
        List<Faction> factionsList = Arrays.asList(factionsArray);
        return factionsList;
    }

}
