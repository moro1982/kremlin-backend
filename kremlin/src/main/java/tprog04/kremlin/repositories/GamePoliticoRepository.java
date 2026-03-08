package tprog04.kremlin.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GamePolitico;

public interface GamePoliticoRepository extends JpaRepository<GamePolitico, Long> {
    
    List<GamePolitico> findByPoliticoNameLike(String name);

    List<GamePolitico> findAllByGame(Game game);
    
}
