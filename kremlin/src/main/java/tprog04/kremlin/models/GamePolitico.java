package tprog04.kremlin.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import tprog04.kremlin.aux_classes.GamePoliticoStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"gameMinistry"})
@EqualsAndHashCode(exclude = {"gameMinistry", "game"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class GamePolitico {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Politico politico;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @OneToOne(mappedBy = "minister")
    private GameMinistry gameMinistry;

    private Integer currentAge;

    // Possible values: 0 (default), 1, 2, 2+ (dead)
    private Integer damage = 0;
    // Total open investigations / Possible values >= 0
    private Integer investigationCount = 0;
    // Open investigations at the beginning of phase 3
    private Integer investigationCountAtPhaseStart = 0;
    // Post absolution restriction
    private Integer immuneToInvestigationsUntilTurn;

    @Enumerated(EnumType.STRING)
    private GamePoliticoStatus status;
    
}