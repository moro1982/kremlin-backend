package tprog04.kremlin.services.influence.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tprog04.kremlin.dto.influence.assigned.AssignedRequestDTO;
import tprog04.kremlin.dto.influence.assigned.AssignedResponseDTO;
import tprog04.kremlin.mapper.InfluenceMapper;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.InfluenceAssigned;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.PlayerRepository;
import tprog04.kremlin.services.influence.AbstractInfluenceService;

@Service
public class AssignedInfluenceService extends AbstractInfluenceService {

    @Autowired
    private GameRepository repoGame;
    @Autowired
    private PlayerRepository repoPlayer;
    @Autowired
    private InfluenceMapper influenceMapper;

    public List<AssignedResponseDTO> getAllAssigned() {
        return repoAssigned.findAll()
                           .stream()
                           .map( assigned -> influenceMapper.toDto(assigned, new AssignedResponseDTO()))
                           .collect(Collectors.toList());
    }

    public AssignedResponseDTO getAssignedById(Long assignID) {
        return influenceMapper.toDto(repoAssigned.findById(assignID).get(), new AssignedResponseDTO());
    }

    public List<AssignedResponseDTO> getAssignedByPlayer(Long playerID) {
        Player player = repoPlayer.findById(playerID).orElse(null);
        if (player == null) {
            return new ArrayList<AssignedResponseDTO>();
        }
        
        List<InfluenceAssigned> entities = this.repoAssigned.findByPlayer(player);
        List<AssignedResponseDTO> assignedList = entities.stream()
                               .map(assigned -> influenceMapper.toDto(assigned, new AssignedResponseDTO()))
                               .collect(Collectors.toList());
        return assignedList;
    }

    public InfluenceAssigned getAssignedByPlayerAndGamePolitico(Player player, GamePolitico gamePol) {
        return this.repoAssigned
                   .findByPlayerAndGamePolitico(player, gamePol)
                   .orElseThrow(
                       () -> new IllegalStateException
                                ("Player has no influence assigned on this Politico.\n")
                   );
    }

    public boolean exceedsTotalMaxAssigned(AssignedRequestDTO dto, Player player) {
        int totalAssignedByPlayer = repoAssigned.findTotalSumByPlayer(player).orElse(0);
        Integer assignedPoints = dto.getPoints();
        if (assignedPoints != null) {
            return (totalAssignedByPlayer + assignedPoints) > 55;
        }
        return false;
    }

    public boolean assignedIsUniqueInRange(AssignedRequestDTO dto, Player player) {
        List<Integer> assignedValues = new ArrayList<>();
        assignedValues = repoAssigned.findByPlayer(player).stream()
                                               .map(InfluenceAssigned::getPoints)
                                               .collect(Collectors.toList());
        Integer assignedPoints = dto.getPoints();
        if ( assignedPoints != null &&
             (assignedValues.contains(assignedPoints) || 
              assignedPoints < 1 || 
              assignedPoints > 10)
            ) 
            return false;
        return true;
    }

    public List<Integer> getPossibleValuesToAssign(Long playerID) {
        
        Player player = repoPlayer.findById(playerID).orElse(null);
        if (player == null) {
            return new ArrayList<>();
        }

        List<Integer> alreadyAssigned = repoAssigned.findByPlayer(player)
                                            .stream()
                                            .map(assigned -> assigned.getPoints())
                                            .collect(Collectors.toList());
        
        List<Integer> range = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            range.add(i+1);
        }

        List<Integer> possibleValues = range.stream()
                                            .filter(value -> !(alreadyAssigned.contains(value)))
                                            .collect(Collectors.toList());
        
        return possibleValues;
    }
    
    public AssignedResponseDTO assignInfluence(AssignedRequestDTO dto) {

        AssignedResponseDTO responseDTO = new AssignedResponseDTO();

        Player player = this.validatePlayer(dto);
        if (player == null) {
            responseDTO = null;
            System.out.println("El ID indicado no corresponde a un Jugador.\n");
            return responseDTO;
        }

        GamePolitico politico = this.validatePolitico(dto);
        if (politico == null) {
            responseDTO = null;
            System.out.println("El ID indicado no corresponde a un Politico válido para esta asignación.\n");
            return responseDTO;
        }

        // Get previous assignment on this Politico by Player (if exists)
        InfluenceAssigned previouslyAssigned = this.alreadyAssigned(player, politico);

        // Check if still under 10 Politicos with assigned influence (if not update/delete)
        if (
            this.numberAssignedPoliticos(player) >= 10 &&
            previouslyAssigned == null
        ) {
            responseDTO = this.influenceMapper.toDto(previouslyAssigned, new AssignedResponseDTO());
            System.out.println("El Jugador ya ha asignado influencia sobre 10 Políticos.\n");
            return responseDTO;
        }

        // Check if assigned value + already assigned so far <= 55
        if (this.exceedsTotalMaxAssigned(dto, player)) {
            responseDTO = null;
            System.out.println("La cantidad de Influencia asignada excede el máximo permitido.\n");
            return responseDTO;
        }

        // Check if assigned value is unique among all assigned values
        if (!this.assignedIsUniqueInRange(dto, player)) {
            responseDTO = null;
            System.out.println("El valor indicado ya fue asignado a otro Político o excede el valor máximo permitido.\n");
            return responseDTO;
        }

        // If Politico has an assignment by this Player...
        if (previouslyAssigned != null) {
            // If value to reassign is null, delete previous assignment.
            if (dto.getPoints() == null) {
                System.out.println("Null value points. Assignment will be anulled.\n");
                this.repoAssigned.delete(previouslyAssigned);
                Game game = player.getGame();
                boolean isReady = game.getReadyPlayers().contains(player.getId());
                if (isReady) {
                    game.getReadyPlayers().remove(player.getId());
                    this.repoGame.save(game);
                }
                responseDTO = null;
            } else {
                // If value to reassign is valid, overwrite previously assigned's value.
                System.out.println(
                    "Player has already assigned influence on this Politico.\n" +
                    "Assigned value wil be overwritten by new value.\n"
                );
                previouslyAssigned.setPoints(dto.getPoints());
                InfluenceAssigned saved = this.repoAssigned.save(previouslyAssigned);
                responseDTO = influenceMapper.toDto(saved, new AssignedResponseDTO());
            }
            return responseDTO;
        }

        // If reached here, create new assignment
        InfluenceAssigned newAssignment = new InfluenceAssigned();
        newAssignment.setPoints(dto.getPoints());
        newAssignment.setPlayer(player);
        newAssignment.setGamePolitico(politico);

        InfluenceAssigned saved = this.repoAssigned.save(newAssignment);
        responseDTO = influenceMapper.toDto(saved, new AssignedResponseDTO());
        return responseDTO;

    }

}
