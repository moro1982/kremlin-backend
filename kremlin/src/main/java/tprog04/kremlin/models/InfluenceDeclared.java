package tprog04.kremlin.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// @Data    --> Lo reemplazamos por lo siguiente:

/* Hasta aquí. */

/* 
Una solución más definitiva al problema de la serialización de ciclos, es usar un DTO
para cada modelo. De esta forma, no exponemos todos los datos cada vez innecesariamente.
*/
/*
    @Data // --> Incluso si usamos DTO, debemos reemplazar @Data por el bloque de arriba 
    (para evitar problemas de serialización)
*/

@Entity
@Table(  
    name = "influence_declared",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "game_politico_id"})
    }
)
public class InfluenceDeclared extends Influence {

    /* 
    Restricciones de unicidad compuesta:
        En ambas subclases (Asignada y Declarada), debe existir una clave única compuesta
        sobre (jugador_id, politico_id) para evitar múltiples asignaciones o declaraciones 
        sobre el mismo político por el mismo jugador.
    */

    // declarada <= asignada

}

/*

b) Validaciones en lógica de negocio (servicios):
Validar que el total de asignación sea exactamente 55 y a 10 políticos distintos.

Validar que un jugador no declare influencia si no tiene suficiente asignación restante.

Validar que no pueda declarar influencia sobre un político si ya hay otro con mayor 
o igual cantidad declarada.

*/