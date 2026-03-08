package tprog04.kremlin.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
// import jakarta.persistence.Entity;
// import jakarta.persistence.Inheritance;
// import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// import jakarta.persistence.MappedSuperclass;

/* Clase ancestra de InfluenciaAsignada e InfluenciaDeclarada */
// @Entity
// @Inheritance(strategy = InheritanceType.JOINED)
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"player", "gamePolitico"})
@EqualsAndHashCode(exclude = {"player", "gamePolitico"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public abstract class Influence {
    
    @Id
    @GeneratedValue( strategy = GenerationType.AUTO)
    private Long id;

    private Integer points;  // declarada <= asignada

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "game_politico_id")
    private GamePolitico gamePolitico;
}


/*

Jugador 1 ── * InfluenciaAsignada * ── 1 Politico
    (máximo 10)                         (varios jugadores pueden asignar)

Jugador 1 ── * InfluenciaDeclarada * ── 1 Politico
    (una por político a la vez)         (varios jugadores pueden declarar)

*/