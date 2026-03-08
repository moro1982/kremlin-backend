package tprog04.kremlin.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tprog04.kremlin.dto.influence.InfluenceResponseDTO;
import tprog04.kremlin.dto.influence.assigned.AssignedRequestDTO;
import tprog04.kremlin.dto.influence.declared.DeclaredRequestDTO;
import tprog04.kremlin.models.Influence;
import tprog04.kremlin.models.InfluenceAssigned;
import tprog04.kremlin.models.InfluenceDeclared;
import tprog04.kremlin.repositories.GamePoliticoRepository;
import tprog04.kremlin.repositories.PlayerRepository;

@Component
public class InfluenceMapper {

    @Autowired
    private PlayerRepository repoPlayer;
    @Autowired
    private GamePoliticoRepository repoGamePolitico;

    public <T extends InfluenceResponseDTO> T toDto(Influence influence, T dto) {
        if (influence == null) {
            return null;
        }
        dto.setId(influence.getId());
        dto.setPoints(influence.getPoints());
        dto.setPlayerId(influence.getPlayer().getId());
        dto.setGamePoliticoId(influence.getGamePolitico().getId());
        return dto;
    }

    public InfluenceAssigned toEntityAssigned(AssignedRequestDTO dto) {
        InfluenceAssigned assigned = new InfluenceAssigned();
        assigned.setPoints(dto.getPoints());
        repoPlayer.findById(dto.getPlayerId()).ifPresent(assigned::setPlayer);
        repoGamePolitico.findById(dto.getGamePoliticoId()).ifPresent(assigned::setGamePolitico);
        return assigned;
    }

    public InfluenceDeclared toEntityDeclared(DeclaredRequestDTO dto) {
        InfluenceDeclared declared = new InfluenceDeclared();
        declared.setPoints(dto.getPoints());
        repoPlayer.findById(dto.getPlayerId()).ifPresent(declared::setPlayer);
        repoGamePolitico.findById(dto.getGamePoliticoId()).ifPresent(declared::setGamePolitico);
        return declared;
    }
    
}
