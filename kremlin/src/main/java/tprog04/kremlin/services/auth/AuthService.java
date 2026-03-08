package tprog04.kremlin.services.auth;

import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tprog04.kremlin.aux_classes.Role;
import tprog04.kremlin.dto.auth.AuthRequest;
import tprog04.kremlin.models.User;
import tprog04.kremlin.repositories.UserRepository;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final MyUserDetailsService userDetailsService;
    private final UserRepository repoUser;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authManager,
                       JwtService jwtService,
                       MyUserDetailsService userDetailsService,
                       UserRepository repoUser,
                       PasswordEncoder passwordEncoder
                      )
    {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.repoUser = repoUser;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(AuthRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRoles(Set.of(Role.PLAYER.toString()));
        
        repoUser.save(user);
    }

    public String login(AuthRequest request) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        return token;
    }

}
