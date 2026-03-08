package tprog04.kremlin.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tprog04.kremlin.aux_classes.MinistryEnum;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GameMinistry;

public interface GameMinistryRepository extends JpaRepository<GameMinistry, Long> {
    List<GameMinistry> findByGameId(Long gameID);
    List<GameMinistry> findByGameAndMinistryName(Game game, MinistryEnum ministryName);
}
