package tprog04.kremlin.services.influence.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tprog04.kremlin.dto.influence.declared.DeclaredRequestDTO;
import tprog04.kremlin.dto.influence.declared.DeclaredResponseDTO;
import tprog04.kremlin.mapper.InfluenceMapper;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.InfluenceAssigned;
import tprog04.kremlin.models.InfluenceDeclared;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.services.influence.AbstractInfluenceService;

@Service
public class DeclaredInfluenceService extends AbstractInfluenceService {

    @Autowired
    private InfluenceMapper influenceMapper;

    public List<InfluenceDeclared> getAllDeclared() {
         return repoDeclared.findAll();
    }

    public DeclaredResponseDTO getDeclaredById(Long declareID) {
        InfluenceDeclared declared = repoDeclared.findById(declareID).orElse(null);
        if (declared == null) {
            return new DeclaredResponseDTO();
        }
        return influenceMapper.toDto(declared, new DeclaredResponseDTO());
    }

    public List<DeclaredResponseDTO> getDeclaredByPlayer(Long playerID) {
        Player player = repoPlayer.findById(playerID).orElse(null);
        if (player == null) {
            return new ArrayList<DeclaredResponseDTO>();
        }
        List<InfluenceDeclared> entities = repoDeclared.findByPlayer(player);
        List<DeclaredResponseDTO> declaredList = 
            entities.stream()
                    .map(declared -> influenceMapper.toDto(declared, new DeclaredResponseDTO()))
                    .collect(Collectors.toList());
        return declaredList;
    }

    public List<InfluenceDeclared> getAllDeclaredOnGamePolitico(GamePolitico gamePol) {
        List<InfluenceDeclared> allDeclared =
            this.repoDeclared.findByGamePolitico(gamePol);
        return allDeclared;
    }

    public InfluenceDeclared getDeclaredByPlayerAndGamePolitico
        (Player player, GamePolitico gamePol)
    {
        InfluenceDeclared prevDeclared = 
            this.repoDeclared.findByPlayerAndGamePolitico(player, gamePol)
                             .orElse(null);
        return prevDeclared;
    }

    public List<Integer> getPossibleValuesToDeclare(DeclaredRequestDTO dto) {

        // Check if Player exists
        Player player = validatePlayer(dto);
        // Validate if Politico exists or is Active
        GamePolitico politico = validatePolitico(dto);
        // Validate if Player has Influence Assigned on Politico
        InfluenceAssigned assigned = repoAssigned.findByPlayerAndGamePolitico(player, politico)
                                                 .orElse(null);
        // If Player or Politico is null, return null response 
        if (player == null || politico == null || assigned == null) {
            System.out.println("El Jugador no existe o no tiene Influencia Asignada sobre el Político (o este no es válido).\n");
            return null;
        }
        // If there's previous Declared Influence, possible values begin at maxDeclared.points + 1
        int beginAt = repoDeclared.findMaxDeclaredByGamePolitico(politico).orElse(0) + 1;
        // Get max possible value to declare 
        // (max Declared points on Politico vs assigned)
        int assignedValue = assigned.getPoints();
        // If beginAt >= assignedValue -> return null
        if (beginAt > assignedValue) {
            System.out.println("El valor máximo Declarado es mayor o igual al valor asignado.\n");
            return null;
        }
        // Now we can generate a possible range of values to Declare.
        List<Integer> possibleDeclareValues = new ArrayList<>();
        for (int i = beginAt; i <= assignedValue; i++) {
            possibleDeclareValues.add(i);
        }

        return possibleDeclareValues;
    }

    public InfluenceDeclared getMaxDeclaredOnGamePolitico(GamePolitico gamePol) {
        List<InfluenceDeclared> allDeclaredOnPolitico = 
            this.repoDeclared.findDeclaredByGamePoliticoOrderByPointsDesc(gamePol);
        if (allDeclaredOnPolitico.isEmpty()) {
            return null;
        }
        InfluenceDeclared maxDeclaredOnPolitico = allDeclaredOnPolitico.get(0);
        return maxDeclaredOnPolitico;
    }

    public DeclaredResponseDTO declareInfluence(DeclaredRequestDTO dto) {
        // Validate if Player exists
        Player player = validatePlayer(dto);
        // Validate if Politico exists or is Active
        GamePolitico politico = validatePolitico(dto);
        // If Player or Politico is null, return null response 
        if (player == null || politico == null) {
            System.out.println("El Jugador o Político no válido o inactivo.\n");
            return null;
        }
        // Find influence assigned by this Player on this Politico
        InfluenceAssigned assigned = repoAssigned.findByPlayerAndGamePolitico(player, politico)
                                                 .orElse(null);
        // If no assignment, null response (can't declare on Politico)
        if (assigned == null) {
            System.out.println("El Jugador no tiene Influencia asignada sobre ese Político.\n");
            return null;
        }
        // If Declared points exceeds Assigned points (or is below 1),
        //  null response (invalid declaration).
        int declaredPoints = dto.getPoints();
        int assignedPoints = assigned.getPoints();
        if (declaredPoints < 1 || declaredPoints > assignedPoints) {
            System.out.println("El valor declarado es menor que 1 o excede la Influencia asignada.\n");
            return null;
        }
        // Find maximum declared points among any declared values on Politico.
        int maxDeclaredPoints = repoDeclared.findMaxDeclaredByGamePolitico(politico).orElse(0);
        // If max declared exists and is lower than declared points.
        if (maxDeclaredPoints != 0 && maxDeclaredPoints >= declaredPoints) {
            System.out.println("El valor declarado no supera el máximo declarado actual.\n");
            return null;
        }
        // If Player has already declared influence on this Politico, update value points.
        InfluenceDeclared prevDeclared = repoDeclared.findByPlayerAndGamePolitico(player, politico).orElse(null);
        if (prevDeclared != null && declaredPoints > prevDeclared.getPoints()) {
            prevDeclared.setPoints(declaredPoints);
            InfluenceDeclared updated = repoDeclared.save(prevDeclared);
            return influenceMapper.toDto(updated, new DeclaredResponseDTO());
        }
        /* If reached here, save new declaration */
        InfluenceDeclared newDeclared = new InfluenceDeclared();
        newDeclared.setPoints(declaredPoints);
        newDeclared.setPlayer(player);
        newDeclared.setGamePolitico(politico);

        InfluenceDeclared saved = repoDeclared.save(newDeclared);
        return influenceMapper.toDto(saved, new DeclaredResponseDTO());

    }

    public void removeDeclaredInfluence(InfluenceDeclared declared) {
        this.repoDeclared.delete(declared);
    }

}
