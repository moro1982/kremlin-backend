package tprog04.kremlin.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tprog04.kremlin.aux_classes.TrialVoteValue;

@Data
@ToString(exclude = {"trial", "voter"})
@EqualsAndHashCode(exclude = {"trial", "voter"})
@Entity
public class TrialVote {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private Trial trial;

    @ManyToOne(optional = false)
    private GamePolitico voter;
    
    @Enumerated(EnumType.STRING)
    private TrialVoteValue vote;

    private Integer turn;
    private Integer phase;

    @Column(nullable = false)
    private boolean cancelled = false;
}
