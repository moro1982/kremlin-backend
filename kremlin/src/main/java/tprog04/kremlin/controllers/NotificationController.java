package tprog04.kremlin.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import tprog04.kremlin.models.Game;
import tprog04.kremlin.models.Player;
import tprog04.kremlin.models.User;
import tprog04.kremlin.repositories.UserRepository;
import tprog04.kremlin.services.notification.PlayerNotificationService;
import tprog04.kremlin.services.validation.ValidationService;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    
    @Autowired
    private PlayerNotificationService notificationService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private UserRepository repoUser;

    @PostMapping("/handshake")
    public ResponseEntity<Void> handshake(HttpServletRequest request) {

        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || 
            !auth.isAuthenticated() ||
            auth.getPrincipal().equals("anonymousUser")
        ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetails ud = (UserDetails) auth.getPrincipal();
        User user = this.repoUser.findByUsername(ud.getUsername())
                                 .orElseThrow();
        
        // Force HttpSession cration
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        session.setAttribute("SSE_SESSION", true);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/game/{gameID}/events")
    public SseEmitter subscribe(
        @PathVariable Long gameID,
        HttpServletRequest request
    ) {

        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new RuntimeException("No active session.");
        }
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("User not in session.");
        }
        
        Game game = this.validationService.validateGameByID(gameID);
        Player player = this.validationService.getPlayerByUserAndGame(user, game);

        SseEmitter emitter = new SseEmitter(0L);    // no timeout
        this.notificationService.register(gameID, player.getId(), emitter);
        return emitter;
    }

}
