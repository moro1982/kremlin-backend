package tprog04.kremlin.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tprog04.kremlin.models.Game;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findById(Long id);
    @Query("""
        SELECT g
        FROM Game g
        JOIN g.players p
        WHERE p.id = :playerId
    """)
    Game findByPlayer(@Param("playerId") Long playerId);
}
