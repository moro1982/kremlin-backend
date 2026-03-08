package tprog04.kremlin.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.InfluenceAssigned;
import tprog04.kremlin.models.Player;

public interface InfluenceAssignedRepository 
extends JpaRepository<InfluenceAssigned, Long> {
    
    List<InfluenceAssigned> findByPlayer(Player player);

    Optional<InfluenceAssigned> findByPlayerAndGamePolitico(Player player, GamePolitico politico);

    List<GamePolitico> findGamePoliticosByPlayer(Player player);
    
    int countByPlayer(Player player);

    @Query("SELECT SUM(inf.points) FROM InfluenceAssigned inf WHERE inf.player = :player")
    Optional<Integer> findTotalSumByPlayer(@Param("player") Player player);

}
