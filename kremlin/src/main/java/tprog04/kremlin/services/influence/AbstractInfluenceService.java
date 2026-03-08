package tprog04.kremlin.services.influence;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.MappedSuperclass;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;
import tprog04.kremlin.dto.influence.InfluenceRequestDTO;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.InfluenceAssigned;
import tprog04.kremlin.models.InfluenceDeclared;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.repositories.GamePoliticoRepository;
import tprog04.kremlin.repositories.InfluenceAssignedRepository;
import tprog04.kremlin.repositories.InfluenceDeclaredRepository;
import tprog04.kremlin.repositories.PlayerRepository;

@MappedSuperclass
public abstract class AbstractInfluenceService {

    @Autowired
    protected PlayerRepository repoPlayer;
    @Autowired
    protected GamePoliticoRepository repoGamePolitico;
    @Autowired
    protected InfluenceAssignedRepository repoAssigned;
    @Autowired
    protected InfluenceDeclaredRepository repoDeclared;

    // If Player exists
    public Player validatePlayer(InfluenceRequestDTO dto) {
        Player player = repoPlayer.findById(dto.getPlayerId()).orElse(null);
        if (player == null)
            System.out.println("El ID no corresponde a un Jugador.\n");
        return player;
    }
    
    // If GamePolitico is in play 
    public GamePolitico validatePolitico(InfluenceRequestDTO dto) {
        GamePolitico politico = repoGamePolitico.findById(dto.getGamePoliticoId()).orElse(null);
        if (politico == null) {
            System.out.println("El ID no corresponde a un Político válido.\n");
            return null;
        }
        if (politico.getStatus() == GamePoliticoStatus.INACTIVE) {
            System.out.println("El Político no está activo (fallecido o fuera de juego).\n");
            return null;
        }
        return politico;
    }
    
    // If Player already assigned Influence on a Politico
    public InfluenceAssigned alreadyAssigned(Player player, GamePolitico politico) {
        InfluenceAssigned assignment = repoAssigned.findByPlayerAndGamePolitico(player, politico).orElse(null);
        return assignment;
    }
    
    // List of Politicos assigned by Player
    public List<GamePolitico> getPoliticosAssignedByPlayer(Player player) {
        List<GamePolitico> politicos = repoAssigned.findGamePoliticosByPlayer(player);
        return politicos;
    }
    
    // Get number of Politicos influenced by Player (max 10)
    public int numberAssignedPoliticos(Player player) {
        int number = repoAssigned.countByPlayer(player);
        return number;
    }

    // Internal use only (no endpoint).
    public InfluenceAssigned discardAllDeclaredInfluence(InfluenceDeclared declared) {
        // Before eliminating declaration, 
        // - save declared value points (will be substracted to assigned influence)
        // - get controller (max declared's Player)
        int discardValue = declared.getPoints();
        Player controller = declared.getPlayer();
        GamePolitico targetGamePolitico = declared.getGamePolitico();
                
        // Get assigned influence for declared influence's Player and Politico
        InfluenceAssigned assigned = this.alreadyAssigned(controller, targetGamePolitico);
        int originalValue = assigned.getPoints();
        int newValue = originalValue - discardValue;
        // Verify if declared == assigned, in which case eliminate assigned too
        if (newValue > 0) {
            assigned.setPoints(newValue);
            InfluenceAssigned modified = this.repoAssigned.save(assigned);
            System.out.println("Influence assigned by player " + controller +
                               " on Politico " + targetGamePolitico +
                               " has been reduced to " + modified.getPoints() + " points.\n");
            this.repoDeclared.delete(declared);
            return modified;
        } else {
            this.repoDeclared.delete(declared);
            this.repoAssigned.delete(assigned);
            System.out.println("Influence assigned by player " + controller +
                               " on Politico " + targetGamePolitico +
                               " has been discarded.\n");
            return null;
        }
    }

    public InfluenceDeclared discardPartialDeclaredInfluence(
        InfluenceDeclared declared,
        int discardValue
    )
    {
        int totalDeclared = declared.getPoints();
        Player controller = declared.getPlayer();
        GamePolitico targetGamePolitico = declared.getGamePolitico();

        // Get assigned influence for declared influence's Player and Politico
        InfluenceAssigned assigned = this.alreadyAssigned(controller, targetGamePolitico);
        int totalAssigned = assigned.getPoints();

        int newDeclaredValue = totalDeclared - discardValue;
        int newAssignedValue = totalAssigned - discardValue;

        if (newAssignedValue > 0) {
            // Modify assigned value
            assigned.setPoints(newAssignedValue);
            this.repoAssigned.save(assigned);
        } else {
            this.repoAssigned.delete(assigned);
        }

        if (newDeclaredValue > 0) {
            // Modify declared value
            declared.setPoints(newDeclaredValue);
            InfluenceDeclared modifiedDeclared = this.repoDeclared.save(declared);
            System.out.println("Influence declared by player " + controller +
                               " on Politico " + targetGamePolitico +
                               " has been reduced to " + modifiedDeclared.getPoints() +
                               " points.\n");
            return modifiedDeclared;
        } else {
            this.repoDeclared.delete(declared);
            System.out.println("Influence declared by player " + controller +
                               " on Politico " + targetGamePolitico +
                               " has been completely discarded.\n");
        }

        return null;
    }

    /* ASSIGNED */
    // Unique Influence points between [1-10]
    // If Player has exceeded 10 assignments on Politicos
    // If Player has exceded total amount of points to Assign (55)

    /* DECLARED */
    // If Declared on Politico <= Assigned on Politico
    // If Declared > Max (Declared Influences on Politico)
}
