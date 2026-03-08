package tprog04.kremlin.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.dto.control.GameMinistryStatusDTO;
import tprog04.kremlin.dto.influence.declared.DeclaredResponseDTO;
import tprog04.kremlin.mapper.InfluenceMapper;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.InfluenceDeclared;
import tprog04.kremlin.repositories.GameMinistryRepository;
import tprog04.kremlin.repositories.GamePoliticoRepository;
import tprog04.kremlin.repositories.InfluenceDeclaredRepository;
import tprog04.kremlin.repositories.PlayerRepository;

@Service
public class ControlPoliticoService {

    @Autowired
    private GameMinistryRepository repoGameMinistry;
    @Autowired
    private PlayerRepository repoPlayer;
    @Autowired
    private GamePoliticoRepository repoGamePolitico;
    @Autowired
    private InfluenceDeclaredRepository repoDeclared;
    @Autowired
    private InfluenceMapper influenceMapper;

    /* Returns Player currently controlling a Politico */
       // (If no influence declared, returns null)
    public Player getControllingPlayer(Long politicoID) {
        
        GamePolitico politico = repoGamePolitico.findById(politicoID).orElse(null);
        if (politico == null) {
            return null;
        }

        List<InfluenceDeclared> declarationsOnPolitico = repoDeclared.findDeclaredByGamePoliticoOrderByPointsDesc(politico);
        if (declarationsOnPolitico.isEmpty()) {
            return null;
        }

        InfluenceDeclared maxDeclared = declarationsOnPolitico.get(0);
        Player controlador = maxDeclared.getPlayer();

        // System.out.println("Ranking de declaraciones sobre el político " + 
        //                    politico.getName() + 
        //                    ":\n");
        // for (InfluenceDeclared influenceDeclared : declarationsOnPolitico) {
        //     System.out.println("Jugador: " + influenceDeclared.getPlayer().getName());
        //     System.out.println("Puntaje declarado: " + influenceDeclared.getPoints());
        // }

        // System.out.println("El Politico " + politico.getName() + 
        //                    " es controlado por el Jugador " + controlador.getName() + 
        //                    " con un total de " + maxDeclared.getPoints() + " puntos.\n");

        return controlador;

    }

    /* Returns list of influence declarations on a Politico */
        // (If no declarations, return empty list)
    public List<DeclaredResponseDTO> getInfluenceDeclarationsOnPolitico(Long politicoID) {
        
        GamePolitico politico = repoGamePolitico.findById(politicoID).orElse(null);
        if (politico == null) {
            List<DeclaredResponseDTO> ret = new ArrayList<DeclaredResponseDTO>();
            return ret;
        }

        List<InfluenceDeclared> declarations = repoDeclared.findDeclaredByGamePoliticoOrderByPointsDesc(politico);
        
        List<DeclaredResponseDTO> declarationsResponse = 
        declarations.stream()
                    .map(declaration -> influenceMapper.toDto(declaration, new DeclaredResponseDTO()))
                    .collect(Collectors.toList());
        
        return declarationsResponse;
    }

    /* Validates if a Politician is controled by a Player */
    public boolean playerControlsPolitico(Player player, GamePolitico gamePolitico) {

        // Get all InfluenceDeclared on a single GamePolitico.
        List<InfluenceDeclared> allDeclarationsOnPolitico = 
            this.repoDeclared.findDeclaredByGamePoliticoOrderByPointsDesc(gamePolitico);

        // If the list of InfluenceDeclared is empty, return false.
        if (allDeclarationsOnPolitico.isEmpty()) {
            return false;
        }

        // Get max InfluenceDeclared value on Politico (first on list).
        InfluenceDeclared maxDeclared = allDeclarationsOnPolitico.get(0);   // (By now, we know it's not null).

        // Return true if Player is the one who declared the max value. 
        return maxDeclared.getPlayer().equals(player);
    }

    /* Returns list of Politicos controlled by a Player */
    public List<GamePolitico> getPoliticosControlledByPlayer(Long playerID) {
        
        Player player = repoPlayer.findById(playerID).orElse(null);
        if (player == null) {
            List<GamePolitico> ret = new ArrayList<GamePolitico>();
            return ret;
        }

        List<GamePolitico> politicos = repoGamePolitico.findAll();
        List<GamePolitico> controlledPoliticos = politicos.stream()
                    .filter(politico -> this.playerControlsPolitico(player, politico))
                    .collect(Collectors.toList());

        return controlledPoliticos;
    }

    public List<GameMinistryStatusDTO> getAllMinistryStatus() {
        return this.repoGameMinistry
                    .findAll()
                    .stream()
                    .map( ministry -> {
                        GamePolitico minister = ministry.getMinister();
                        
                        Long ministerID = (minister != null)
                                            ? minister.getId()
                                            : null;

                        Player controller = null;
                        Long controllerID = null;

                        if (ministerID != null) {
                            controller = this.getControllingPlayer(ministerID);
                            if (controller != null) {
                                controllerID = controller.getId();
                            }
                        }

                        GameMinistryStatusDTO status = new GameMinistryStatusDTO();
                        status.setGameMinistryID(ministry.getId());
                        status.setMinisterID(ministerID);
                        status.setControllerID(controllerID);
                        return status;
                    })
                    .collect(Collectors.toList());
    }

}
