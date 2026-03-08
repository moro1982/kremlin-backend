package tprog04.kremlin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tprog04.kremlin.models.Trial;

public interface TrialRepository extends JpaRepository<Trial, Long> { }
