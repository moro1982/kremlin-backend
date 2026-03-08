package tprog04.kremlin.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tprog04.kremlin.dto.gameMinistry.GameMinistryRequestDTO;
import tprog04.kremlin.dto.gameMinistry.GameMinistryResponseDTO;
import tprog04.kremlin.models.GameMinistry;
import tprog04.kremlin.repositories.GamePoliticoRepository;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.MinistryRepository;

@Component
public class GameMinistryMapper {

    @Autowired
    private MinistryRepository repoMinistry;
    @Autowired
    private GameRepository repoGame;
    @Autowired
    private GamePoliticoRepository repoGamePolitico;
    @Autowired
    private MinistryMapper minMapper;
    
    public GameMinistryResponseDTO toDto(GameMinistry gameMin) {
        GameMinistryResponseDTO dto = new GameMinistryResponseDTO();
        dto.setId(gameMin.getId());
        dto.setMinistryDTO( this.minMapper.toDto(gameMin.getMinistry()) );
        dto.setGameID(gameMin.getGame().getId());
        dto.setMinisterID(gameMin.getMinister() != null
                            ? gameMin.getMinister().getId()
                            : null
                         );
        dto.setVacant(gameMin.isVacant());
        dto.setPurgeModifier(gameMin.getPurgeModifier());
        return dto;
    }

    public GameMinistry toEntity(GameMinistryRequestDTO dto) {
        GameMinistry gameMin = new GameMinistry();
        gameMin.setMinistry(repoMinistry.findById(dto.getMinistryID()).get());
        gameMin.setGame(repoGame.findById(dto.getGameID()).get());
        if (dto.getMinisterID() != null) {
            repoGamePolitico.findById(dto.getMinisterID()).ifPresentOrElse(gameMin::setMinister, null);
        }
        gameMin.setVacant(gameMin.getMinister() == null);
        gameMin.setPurgeModifier(dto.getPurgeModifier());
        return gameMin;
    }
}
