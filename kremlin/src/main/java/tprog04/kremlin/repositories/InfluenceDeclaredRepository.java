package tprog04.kremlin.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.InfluenceDeclared;
import tprog04.kremlin.models.Player;

public interface InfluenceDeclaredRepository 
extends JpaRepository<InfluenceDeclared, Long> {
    
    List<InfluenceDeclared> findByPlayer(Player player);

    List<InfluenceDeclared> findByGamePolitico(GamePolitico politico);

    Optional<InfluenceDeclared> findByPlayerAndGamePolitico(Player player, GamePolitico politico);

    // Get maximun Influence value declared on a GamePolitico
    @Query("SELECT MAX(inf.points) FROM InfluenceDeclared inf WHERE inf.gamePolitico = :gamePolitico")
    Optional<Integer> findMaxDeclaredByGamePolitico(@Param("gamePolitico") GamePolitico gamePolitico);

    // Get all Influence values declared by all Players on a Politico (sorted by value desc)
    List<InfluenceDeclared> findDeclaredByGamePoliticoOrderByPointsDesc(@Param("politico") GamePolitico politico);

}
