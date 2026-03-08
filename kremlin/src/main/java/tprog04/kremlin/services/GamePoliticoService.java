package tprog04.kremlin.services;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;
import tprog04.kremlin.aux_classes.MinistryEnum;
import tprog04.kremlin.dto.gamePolitico.GamePoliticoRequestDTO;
import tprog04.kremlin.dto.gamePolitico.GamePoliticoResponseDTO;
import tprog04.kremlin.mapper.GamePoliticoMapper;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GameMinistry;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.repositories.GamePoliticoRepository;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.PoliticoRepository;

@Service
public class GamePoliticoService {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private PoliticoRepository politicoRepository;
    @Autowired
    private GamePoliticoRepository gamePoliticoRepository;
    @Autowired
    private GamePoliticoMapper gamePolMapper;

    public GamePoliticoResponseDTO getGamePoliticoByID(Long id) {
        GamePolitico found = this.gamePoliticoRepository.findById(id).get();
        return gamePolMapper.toDto(found);
    }

    /* CHEQUEAR (si anda el metodo del repo) */
    public List<GamePolitico> getPoliticosByGameID(Long gameID) {
        Game game = this.gameRepository.findById(gameID)
                                       .orElseThrow(
                                            () -> new IllegalStateException("Invalid Game ID.\n")
                                       );
        return gamePoliticoRepository.findAllByGame(game)
                                     .stream()
                                     .collect(Collectors.toList());
    }

    /* Finds the first 8 Ministers (Politburo members -> Can age and vote) */
    public List<GamePolitico> getPolitburoMembers(Game game) {
        List<GamePolitico> gamePoliticos = this.getPoliticosByGameID(game.getId());
        List<GamePolitico> politburoMembers = 
            gamePoliticos.stream()
                         .filter(gamePol -> gamePol.getGameMinistry() != null
                                            &&
                                            !gamePol.getGameMinistry().getMinistry().getName()
                                                    .equals(MinistryEnum.CANDIDATE)
                                            &&
                                            !gamePol.getGameMinistry().getMinistry().getName()
                                                    .equals(MinistryEnum.PEOPLE)
                         )
                         .collect(Collectors.toList());
        return politburoMembers;
    }

    public List<GamePolitico> getPoliticosInSiberia(Game game) {
        List<GamePolitico> gamePoliticos = this.getPoliticosByGameID(game.getId());
        List<GamePolitico> inSiberia = 
            gamePoliticos.stream()
                         .filter(gamePol -> gamePol.getStatus() != null)
                         .filter(gamePol -> gamePol.getStatus()
                                                   .equals(GamePoliticoStatus.IN_SIBERIA)
                         )
                         .collect(Collectors.toList());
        return inSiberia;
    }

    public GamePolitico saveGamePolitico(GamePolitico gamePol) {
        GamePolitico saved = this.gamePoliticoRepository.save(gamePol);
        return saved;
    }

    public GamePolitico createSingleGamePolitico(GamePoliticoRequestDTO dto) {
        GamePolitico entity = gamePolMapper.toEntity(dto);
        GamePolitico saved = gamePoliticoRepository.save(entity);
        return saved;
    }

    public List<GamePolitico> createManyGamePoliticos( List<GamePoliticoRequestDTO> dtos )
    {
        List<GamePolitico> entities = dtos.stream()
                                          .map(gamePolMapper::toEntity)
                                          .collect(Collectors.toList());

        List<GamePolitico> saved = gamePoliticoRepository.saveAll(entities);

        return saved;
    }

    public List<GamePolitico> loadGamePoliticos(Long gameID) {

        Game currentGame = this.gameRepository.findById(gameID).get();
        List<GamePolitico> loaded = this.getPoliticosByGameID(gameID);
        if (!loaded.isEmpty()) {
            System.out.println("Políticos already loaded for this Game.\n");
            return loaded;
        }

        List<GamePolitico> gamePoliticos = this.politicoRepository
                                               .findAll()
                                               .stream()
                                               .map( (politico) -> {
                                                    GamePolitico gamePol = new GamePolitico();
                                                    gamePol.setPolitico(politico);
                                                    gamePol.setGame(currentGame);
                                                    gamePol.setCurrentAge(politico.getInitialAge());
                                                    return gamePol;
                                               })
                                               .collect(Collectors.toList());

        List<GamePolitico> saved = this.gamePoliticoRepository.saveAll(gamePoliticos);

        return saved;
    }

    public GamePoliticoResponseDTO sendToHospital(GamePolitico targetGamePolitico) {
        targetGamePolitico.setStatus(GamePoliticoStatus.AT_HOSPITAL);
        GamePolitico saved = this.gamePoliticoRepository.save(targetGamePolitico);
        return this.gamePolMapper.toDto(saved);
    }

    public GamePolitico exitHospital(GamePolitico targetGamePolitico) {
        targetGamePolitico.setStatus(GamePoliticoStatus.ACTIVE);
        GamePolitico saved = this.gamePoliticoRepository.save(targetGamePolitico);
        return saved;
    }

    public GamePoliticoResponseDTO sendToSiberia(GamePolitico targetGamePolitico) {        
        targetGamePolitico.setStatus(GamePoliticoStatus.IN_SIBERIA);
        GamePolitico savedPolitico = this.gamePoliticoRepository.save(targetGamePolitico);
        return this.gamePolMapper.toDto(savedPolitico);
    }

    public GamePolitico ageGamePolitico(GamePolitico gamePolitico) {
        int agingFactor = 0;
        // If PARTY_CHIEF, +1
        if (gamePolitico.getGameMinistry().getMinistry().getName()
                        .equals(MinistryEnum.PARTY_CHIEF)
        ) { agingFactor += 1; }
        // If Politico isn't at Hospital, add +1 for each damage point
        if (!gamePolitico.getStatus().equals(GamePoliticoStatus.AT_HOSPITAL)) {
            agingFactor += gamePolitico.getDamage();
        }

        // If Politico is under investigation, add +1 for each investigation point
        agingFactor += gamePolitico.getInvestigationCount();

        // Is in disadvantage Ministry -> +1
        if (this.isInDisadvantage(gamePolitico)) {
            agingFactor += 1;
        }
        // Is in advantage Ministry -> -1
        if (this.isInAdvantage(gamePolitico)) {
            agingFactor -= 1;
        }
        // Apply agingFactor and currentAge never below initialAge
        int minAge = gamePolitico.getPolitico().getInitialAge();
        gamePolitico.setCurrentAge(
            Math.max(minAge, gamePolitico.getCurrentAge() + agingFactor)
        );
        // If older than 95, GamePolitico dies (-> INACTIVE)
        if (gamePolitico.getCurrentAge() > 95) {
            gamePolitico.setStatus(GamePoliticoStatus.INACTIVE);
        }
        // Save
        GamePolitico saved = this.gamePoliticoRepository.save(gamePolitico);
        return saved;
    }
    
    // If minister is in a Ministry of his advantage
    public boolean isInAdvantage(GamePolitico minister) {
        GameMinistry assignedMinistry = minister.getGameMinistry();
        if (assignedMinistry == null) {
            throw new IllegalStateException("Minister must be assigned to a Ministry.\n");
        }
        MinistryEnum assignedMinistryName = assignedMinistry.getMinistry().getName();
        if (minister.getPolitico().getAdvantage().equals(assignedMinistryName)) {
            return true;
        }
        return false;
    }

    // If minister is in a Ministry of his disadvantage
    public boolean isInDisadvantage(GamePolitico minister) {
        GameMinistry assignedMinistry = minister.getGameMinistry();
        if (assignedMinistry == null) {
            throw new IllegalStateException("Minister must be assigned to a Ministry.\n");
        }
        MinistryEnum assignedMinistryName = assignedMinistry.getMinistry().getName();
        if (assignedMinistryName != null) {
            if (minister.getPolitico().getDisadvantage() == assignedMinistryName) {
                return true;
            }
        }
        return false;
    }

}
