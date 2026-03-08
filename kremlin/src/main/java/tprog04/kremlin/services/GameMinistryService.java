package tprog04.kremlin.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;
import tprog04.kremlin.aux_classes.MinistryEnum;
import tprog04.kremlin.dto.gameMinistry.GameMinistryRequestDTO;
import tprog04.kremlin.dto.gameMinistry.GameMinistryResponseDTO;
import tprog04.kremlin.models.ActionInstance;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GameMinistry;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.repositories.GameRepository;
import tprog04.kremlin.repositories.GameMinistryRepository;
import tprog04.kremlin.repositories.GamePoliticoRepository;
import tprog04.kremlin.repositories.MinistryRepository;
import tprog04.kremlin.mapper.GameMinistryMapper;

@Service
public class GameMinistryService {

    @Autowired
    private GameRepository repoGame;
    @Autowired
    private MinistryRepository repoMin;
    @Autowired
    private GameMinistryRepository repoGameMin;
    @Autowired
    private GamePoliticoRepository repoGamePol;
    @Autowired
    private GameMinistryMapper gameMinMapper;

    public static final List<MinistryEnum> PURGE_MINISTRIES = List.of(
        MinistryEnum.KGB_HERO,
        MinistryEnum.IDEOLOGY,
        MinistryEnum.PARTY_CHIEF,
        MinistryEnum.INDUSTRY
    );

    public static final List<MinistryEnum> SPY_INVESTIGATION_MINISTRIES = List.of(
        MinistryEnum.DEFENSE,
        MinistryEnum.FOREIGN,
        MinistryEnum.KGB_HERO,
        MinistryEnum.PARTY_CHIEF,
        MinistryEnum.INDUSTRY
    );

    public static final List<MinistryEnum> ALLOWED_VOTING_MINISTRIES = List.of(
        MinistryEnum.PARTY_CHIEF,
        MinistryEnum.KGB_HERO,
        MinistryEnum.FOREIGN,
        MinistryEnum.DEFENSE,
        MinistryEnum.IDEOLOGY,
        MinistryEnum.INDUSTRY,
        MinistryEnum.ECONOMY,
        MinistryEnum.SPORTS
    );

    public static final List<MinistryEnum> NEGATE_CONDEMNATION_PROMOTERS = List.of(
        MinistryEnum.PARTY_CHIEF,
        MinistryEnum.KGB_HERO,
        MinistryEnum.FOREIGN,
        MinistryEnum.DEFENSE
    );

    @Transactional
    public GameMinistry saveGameMinistry(GameMinistry gameMin) {
        GameMinistry saved = this.repoGameMin.save(gameMin);
        return saved;
    }

    public GameMinistry getGameMinistryByID( Long gameID ) {
        GameMinistry found = 
            this.repoGameMin.findById(gameID).orElseThrow(
                                                () -> new IllegalStateException("Invalid ID.\n")
                                             );
        return found;
    }

    public List<GameMinistry> getGameMinistriesByGame( Long gameID ) {
        boolean exists = this.repoGame.existsById(gameID);
        if (!exists) {
            throw new NullPointerException("Invalid Game ID.\n");
        }
        return repoGameMin.findByGameId(gameID)
                          .stream()
                          .collect(Collectors.toList());
    }

    public List<GameMinistry> getGameMinistriesByGameAndMinistryName
        (Game game, MinistryEnum ministryName)
    {
        return this.repoGameMin.findByGameAndMinistryName(game, ministryName);
    }

    public List<MinistryEnum> getAllowedMinistriesByActionType( ActionType type ) {

        switch (type) {
            case PURGE_ATTEMPT:
                return GameMinistryService.PURGE_MINISTRIES;
            case BEGIN_INVESTIGATION:
                return GameMinistryService.SPY_INVESTIGATION_MINISTRIES;
            case REMOVE_INVESTIGATION:
                return GameMinistryService.SPY_INVESTIGATION_MINISTRIES;
            case OPEN_TRIAL:
                return GameMinistryService.SPY_INVESTIGATION_MINISTRIES;
            case CAST_TRIAL_VOTE:
                return GameMinistryService.ALLOWED_VOTING_MINISTRIES;
            default:
                return null;
        }
        
    }

    public boolean canVote(GameMinistry gameMin) {
        return !gameMin.isVacant() && 
               gameMin.getMinister().getStatus() == GamePoliticoStatus.ACTIVE &&
               GameMinistryService.ALLOWED_VOTING_MINISTRIES.contains(
                    gameMin.getMinistry().getName()
               );
    }

    public GameMinistry createSingleGameMinistry(GameMinistryRequestDTO dto) {
        GameMinistry entity = gameMinMapper.toEntity(dto);
        GameMinistry saved = repoGameMin.save(entity);
        return saved;
    }

    public List<GameMinistry> createManyGameMinistries(List<GameMinistryRequestDTO> dtos)
    {
        List<GameMinistry> entities = dtos.stream()
                                          .map(gameMinMapper::toEntity)
                                          .collect(Collectors.toList());

        List<GameMinistry> saved = repoGameMin.saveAll(entities);

        return saved;
    }

    public GameMinistryResponseDTO assignMinister( Long gameMinID, Long gamePolID ) {
        
        GameMinistry gameMinistry = repoGameMin.findById(gameMinID)
                                               .orElseThrow(
                                                    () -> new RuntimeException(
                                                        "Ministerio no encontrado con el ID: " + gameMinID + " para esta partida.\n"
                                                    )
                                               );
        
        GamePolitico newMinister = repoGamePol.findById(gamePolID)
                                              .orElseThrow(
                                                () -> new RuntimeException(
                                                    "Político no encontrado con el id: " + gamePolID + " para esta partida.\n"
                                                )
                                              );
                                        
        gameMinistry.setMinister(newMinister);
        gameMinistry.setVacant(false);
        newMinister.setGameMinistry(gameMinistry);

        GameMinistry updated = repoGameMin.save(gameMinistry);
        return gameMinMapper.toDto(updated);

    }

    public GameMinistryResponseDTO removeMinister( Long gameMinID ) {
        
        GameMinistry gameMinistry = repoGameMin.findById(gameMinID).get();
        GamePolitico minister = gameMinistry.getMinister();

        minister.setGameMinistry(null);
        gameMinistry.setMinister(null);
        gameMinistry.setVacant(true);

        this.repoGamePol.save(minister);
        GameMinistry updated = repoGameMin.save(gameMinistry);
        
        return gameMinMapper.toDto(updated);
    }

    public List<GameMinistry> loadGameMinistries( Long gameID ) {
        Game myGame = this.repoGame.findById(gameID).orElse(null);
        if (myGame == null) {
            System.out.println("ID de juego inválido.\n");
            return null;
        }
        
        List<GameMinistryRequestDTO> gameMinDTOs = this.repoMin.findAll()
                                                        .stream()
                                                        .map( min -> {
                                                            GameMinistryRequestDTO gameMinDTO = new GameMinistryRequestDTO();
                                                            gameMinDTO.setMinistryID(min.getId());
                                                            gameMinDTO.setGameID(gameID);
                                                            gameMinDTO.setMinisterID(null);
                                                            gameMinDTO.setVacant(true);
                                                            gameMinDTO.setPurgeModifier(0);

                                                            return gameMinDTO;
                                                        })
                                                        .collect(Collectors.toList());
        
        return this.createManyGameMinistries(gameMinDTOs);
    }

    public List<GameMinistry> assignInitialMinisters( Long gameID ) {
        // Validate Game
        Game myGame = this.repoGame.findById(gameID).orElse(null);
        if (myGame == null) {
            System.out.println("Invalid Game ID.\n");
            return null;
        }
        // Validate GameMinistries
        List<GameMinistry> currentGameMinistries = this.repoGameMin.findByGameId(gameID);
        if (currentGameMinistries.isEmpty()) {
            System.out.println("Game Ministries not loaded yet.\n");
            return null;
        }
        // Validate vacant Ministries
        List<GameMinistry> vacantMinistries = currentGameMinistries.stream()
                                                                   .filter(GameMinistry::isVacant)
                                                                   .collect(Collectors.toList());
        if (vacantMinistries.isEmpty()) {
            System.out.println("Ministries occupied with initial Ministers.\n");
            return vacantMinistries.stream()
                                   .collect(Collectors.toList());
        }

        // Validate GamePoliticos
        List<GamePolitico> currentGamePoliticos = this.repoGamePol.findAllByGame(myGame);
        if (currentGamePoliticos.size() < 17) {
            throw new IllegalStateException("Insufficient number of Politicos.\n");
        }
        // Shuffle and assign GamePoliticos to GameMinistries
        Collections.shuffle(currentGamePoliticos);
        List<GamePolitico> selectedGamePoliticos = currentGamePoliticos.subList(0, 17);
        for (int i = 0; i < 17; i++) {
            GameMinistry gameMin = currentGameMinistries.get(i);
            GamePolitico gamePol = selectedGamePoliticos.get(i);
            gameMin.setMinister(gamePol);
            gameMin.setVacant(false);
            gamePol.setGameMinistry(gameMin);
            gamePol.setStatus(GamePoliticoStatus.ACTIVE);
        }
        // Save all and return
        this.repoGamePol.saveAll(selectedGamePoliticos);
        List<GameMinistry> saved = repoGameMin.saveAll(currentGameMinistries);
        return saved.stream()
                    .collect(Collectors.toList());

    }
 
    public List<MinistryEnum> resolveAuthorizedMinistryForActionType( ActionInstance action ) {
        Game game = action.getGame();
        ActionType actionType = action.getType();
        List<MinistryEnum> allowedMinistryEnum = new ArrayList<>();
        switch (actionType) {
            case PURGE_ATTEMPT:
                MinistryEnum purgeMinistry = 
                    this.loopPossibleMinistries(game, PURGE_MINISTRIES);
                allowedMinistryEnum.add(purgeMinistry);
                return allowedMinistryEnum;
            case BEGIN_INVESTIGATION:
                MinistryEnum beginInvestigationMinistry =
                    this.loopPossibleMinistries(game, SPY_INVESTIGATION_MINISTRIES);
                allowedMinistryEnum.add(beginInvestigationMinistry);
                return allowedMinistryEnum;
            case REMOVE_INVESTIGATION:
                MinistryEnum removeInvestigationMinistry =
                    this.loopPossibleMinistries(game, SPY_INVESTIGATION_MINISTRIES);
                allowedMinistryEnum.add(removeInvestigationMinistry);
                return allowedMinistryEnum;
            case OPEN_TRIAL:
                MinistryEnum openTrialMinistry =
                    this.loopPossibleMinistries(game, SPY_INVESTIGATION_MINISTRIES);
                allowedMinistryEnum.add(openTrialMinistry);
                return allowedMinistryEnum;
            case CAST_TRIAL_VOTE:
                return ALLOWED_VOTING_MINISTRIES;
            default:
                return null;
        }
    }

    private MinistryEnum loopPossibleMinistries( Game game, List<MinistryEnum> chain ) {
        for (MinistryEnum ministry : chain) {
            List<GameMinistry> gameMins = this.getGameMinistriesByGameAndMinistryName(game, ministry);
            GameMinistry gameMin = gameMins.get(0);
            // Check if GameMinistry exists
            if (gameMin == null) continue;
            // Check if vacant GameMinistry
            if (gameMin.getMinister() == null) continue;
            // Check if Minister in Hospital
            if (gameMin.getMinister().getStatus() == GamePoliticoStatus.AT_HOSPITAL) continue;
            // First one to pass validations, is authorized
            return ministry;
        }
        return null;    // Nobody 
    }

    public List<GameMinistry> loopPeopleMinistries( Game game ) {
        List<GameMinistry> peopleMinistries = 
            this.repoGameMin.findByGameAndMinistryName(game, MinistryEnum.PEOPLE);
        List<GameMinistry> availablePeople = new ArrayList<>();
        for (GameMinistry peopleMin : peopleMinistries) {
            if (!peopleMin.isVacant()) continue;
            if (peopleMin.getMinister() != null) continue;
            availablePeople.add(peopleMin);
        }
        return availablePeople;
    }
}
