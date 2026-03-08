package tprog04.kremlin.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.models.User;

public interface PlayerRepository extends JpaRepository<Player, Long>{
    List<Player> findPlayersByGameId(Long gameID);
    List<Player> findPlayersByUser(User user);
    Optional<Player> findByUserAndGame(User user, Game game);
}
