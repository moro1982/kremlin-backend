package tprog04.kremlin.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tprog04.kremlin.aux_classes.ActionStatus;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.models.ActionInstance;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.GamePolitico;
import tprog04.kremlin.models.Player;

public interface ActionInstanceRepository extends JpaRepository<ActionInstance, Long> {
    List<ActionInstance> findByGameAndStatus(Game game, ActionStatus status);
    List<ActionInstance> findByGameAndActor(Game game, Player actor);
    boolean existsByGameAndTypeAndTargetGamePoliticoAndTurn(
        Game game, ActionType type, GamePolitico targetGamePolitico, int turn
    );
}
