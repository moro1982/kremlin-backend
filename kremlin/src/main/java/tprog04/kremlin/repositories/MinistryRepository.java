package tprog04.kremlin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tprog04.kremlin.models.Ministry;

public interface MinistryRepository extends JpaRepository<Ministry, Long> { }
