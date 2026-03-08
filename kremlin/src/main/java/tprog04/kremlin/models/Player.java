package tprog04.kremlin.models;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tprog04.kremlin.aux_classes.Faction;

@Data
@ToString(exclude = {"assigned", "declared", "game"})
@EqualsAndHashCode(exclude = {"assigned", "declared", "game"})
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_id"})
)
public class Player {

    @Id
    @GeneratedValue( strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Faction faction;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @OneToMany(mappedBy = "player")
    private Set<InfluenceAssigned> assigned = new HashSet<>();

    @OneToMany(mappedBy = "player")
    private Set<InfluenceDeclared> declared = new HashSet<>();

}
