package tprog04.kremlin.models;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tprog04.kremlin.aux_classes.TrialResult;
import tprog04.kremlin.aux_classes.TrialStatus;

@Data
@ToString(exclude = {"game", "accused", "prosecutor", "votes"})
@EqualsAndHashCode(exclude = {"game", "accused", "prosecutor", "votes"})
@Entity
public class Trial {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private Game game;

    @ManyToOne(optional = false)
    private GamePolitico accused;

    @ManyToOne(optional = false)
    private GamePolitico prosecutor;

    @Enumerated(EnumType.STRING)
    private TrialStatus status;

    @Enumerated(EnumType.STRING)
    private TrialResult result;

    @OneToMany(mappedBy = "trial", cascade = CascadeType.ALL)
    private List<TrialVote> votes = new ArrayList<>();

    private Integer innocentVotes;

    private Integer guiltyVotes;

    private Integer investigationCountAtStart;

    private Integer turn;

}
