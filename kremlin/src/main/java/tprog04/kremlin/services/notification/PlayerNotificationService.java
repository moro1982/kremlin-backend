package tprog04.kremlin.services.notification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tprog04.kremlin.aux_classes.SseEventType;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.repositories.GameRepository;

@Service
public class PlayerNotificationService {
    
    @Autowired
    private GameRepository repoGame;

    // Emitter by Player
    private final Map<Long, Map<Long, List<SseEmitter>>> gameEmitters = new ConcurrentHashMap<>();

    private void removeEmitter(Long gameID, Long playerID, SseEmitter emitter) {

        Map<Long, List<SseEmitter>> players = gameEmitters.get(gameID);
        if (players == null) return;

        List<SseEmitter> emitters = players.get(playerID);
        if (emitters == null) return;

        emitters.remove(emitter);

        if (emitters.isEmpty()) {
            players.remove(playerID);
        }

        if (players.isEmpty()) {
            gameEmitters.remove(gameID);
        }
    }
    
    private void sendGlobalEvent(Long gameID, String eventName, Object data) {

        Map<Long, List<SseEmitter>> players = gameEmitters.get(gameID);
        if (players == null) return;

        for (Map.Entry<Long, List<SseEmitter>> entry : players.entrySet()) {
            
            Long playerID = entry.getKey();
            
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(
                        SseEmitter.event().name(eventName).data(data)
                    );
                } catch (Exception e) {
                    this.removeEmitter(gameID, playerID, emitter);
                }
            }
        }
    }

    private void sendPrivateEvent(
        Long gameID,
        Long playerID,
        String eventName,
        Object data
    )
    {
        Map<Long, List<SseEmitter>> players = gameEmitters.get(gameID);
        if (players == null) return;

        List<SseEmitter> emitters = players.get(playerID);
        if (emitters == null) return;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                this.removeEmitter(gameID, playerID, emitter);
            }
        }
    }

    public void register(Long gameID, Long playerID, SseEmitter emitter) {

        gameEmitters.computeIfAbsent(gameID, g -> new ConcurrentHashMap<>())
                    .computeIfAbsent(playerID, p -> new CopyOnWriteArrayList<>())
                    .add(emitter);

        emitter.onCompletion( () -> this.removeEmitter(gameID, playerID, emitter) );
        emitter.onTimeout( () -> this.removeEmitter(gameID, playerID, emitter) );
        emitter.onError( err -> this.removeEmitter(gameID, playerID, emitter) );
    }

    // Inform the frontend that Game status changed
    public void broadcastGameUpdate(Game game, String reason) {
        game.notifyUpdate();
        this.repoGame.save(game);

        this.sendGlobalEvent(
            game.getId(),
            SseEventType.GAME_UPDATE.name(),
            Map.of(
                "reason", reason,
                "updateCounter", game.getUpdateCounter()
            )
        );

        /* LEGACY */
        // System.out.println("[NOTIFY] Game " + game.getId() + " updated (" + reason + ")");
    }

    // Send PUBLIC message to ALL Player(s)
    public void notifyAllPlayers(Game game, SseEventType type, Object payload) {

        this.sendGlobalEvent(
            game.getId(), 
            type.name(),
            payload
        );
    }

    // Send PRIVATE message to a SINGLE Player
    public void notifySinglePlayer(Game game, Player player, SseEventType type, Object payload) {

        this.sendPrivateEvent(
            game.getId(), 
            player.getId(), 
            type.name(), 
            payload
        );
    }

    /* LEGACY */
    // Send message to Player(s)
    // public void notifyPlayers(Game game, String message) {
    //     game.getPlayers().forEach(player -> {
    //         notifications.computeIfAbsent(player.getId(), k -> new ArrayList<>())
    //                      .add(message);
    //     });
    // }

    /* LEGACY */
    // public List<String> getPlayerNotifications(Long playerID) {
    //     return this.notifications.getOrDefault(playerID, Collections.emptyList());
    // }

    /* LEGACY */
    // public void clearNotifications(Long playerID) {
    //     this.notifications.remove(playerID);
    // }
}
