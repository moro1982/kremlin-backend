package tprog04.kremlin.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tprog04.kremlin.repositories.UserRepository;
import tprog04.kremlin.services.auth.MyUserDetailsService;

@Configuration
public class ApplicationConfig {

    @Autowired
    private UserRepository repoUser;

    public ApplicationConfig(UserRepository repo) {
        this.repoUser = repo;
    }

    @Bean
    public MyUserDetailsService userDetailsService() {
        return new MyUserDetailsService(repoUser);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
                                        throws Exception
    {
        return config.getAuthenticationManager();
    }
}
