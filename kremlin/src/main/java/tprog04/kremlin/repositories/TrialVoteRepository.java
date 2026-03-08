package tprog04.kremlin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tprog04.kremlin.models.TrialVote;

public interface TrialVoteRepository extends JpaRepository<TrialVote, Long> { }
