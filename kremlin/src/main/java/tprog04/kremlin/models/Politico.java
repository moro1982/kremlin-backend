package tprog04.kremlin.models;

import lombok.Getter;
import lombok.Setter;
import tprog04.kremlin.aux_classes.MinistryEnum;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
// import jakarta.persistence.OneToOne;
// import lombok.EqualsAndHashCode;
// import lombok.ToString;

/* Una solución más definitiva al problema de la serialización de ciclos, es usar un DTO para cada modelo. De esta forma, no exponemos todos los datos cada vez innecesariamente. */

// @Data    --> Lo reemplazamos por lo siguiente:
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// @ToString(exclude = {"advantage", "disadvantage"})
// @EqualsAndHashCode(exclude = {"advantage", "disadvantage"})
/* Hasta aquí */
// @Data // --> Incluso si usamos DTO, debemos reemplazar @Data por el bloque de arriba (para evitar problemas de serialización)
@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Politico {
    @Id
    @GeneratedValue( strategy = GenerationType.AUTO)
    private long id;

    // Basic
    private String name;
    private String alias;
    private int initialAge;
    
    @Enumerated(EnumType.STRING)
    private MinistryEnum advantage;

    @Enumerated(EnumType.STRING)
    private MinistryEnum disadvantage;
    
    // private String imageID;
}
