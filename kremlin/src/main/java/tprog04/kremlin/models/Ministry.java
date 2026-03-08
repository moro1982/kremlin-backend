package tprog04.kremlin.models;

import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import tprog04.kremlin.aux_classes.ActionType;
import tprog04.kremlin.aux_classes.MinistryEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"actions"})
@EqualsAndHashCode(exclude = {"actions"})
@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Ministry {
    @Id
    @GeneratedValue( strategy = GenerationType.AUTO )
    private long id;
    
    @Enumerated(EnumType.STRING)
    private MinistryEnum name;
    
    private int purgeNr;

    @ElementCollection(targetClass = ActionType.class)
    @CollectionTable(name = "ministry_action", joinColumns = @JoinColumn(name = "ministry_id"))
    @Enumerated(EnumType.STRING)
    private Set<ActionType> actions = new HashSet<>();

}
