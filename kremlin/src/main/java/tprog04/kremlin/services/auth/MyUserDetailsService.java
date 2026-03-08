package tprog04.kremlin.services.auth;

import org.springframework.beans.factory.annotation.Autowired;
import tprog04.kremlin.models.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tprog04.kremlin.repositories.UserRepository;

public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository repoUser;

    public MyUserDetailsService(UserRepository repo) {
        this.repoUser = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = repoUser.findByUsername(username)
                         .orElseThrow(() -> new UsernameNotFoundException("User not found."));
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPassword())  // already BCrypt encrypted
                .roles(u.getRoles().toArray(new String[0]))
                .build();
    }
}
