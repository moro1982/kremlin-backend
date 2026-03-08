package tprog04.kremlin.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tprog04.kremlin.models.Politico;

public interface PoliticoRepository extends JpaRepository<Politico, Long>{
    List<Politico> findByNameLike(String nombre);
}
