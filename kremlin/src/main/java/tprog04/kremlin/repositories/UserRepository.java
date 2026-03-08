package tprog04.kremlin.repositories;

import java.util.Optional;
import tprog04.kremlin.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
