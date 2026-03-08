package tprog04.kremlin.dto.ministry;

import java.util.Set;
import lombok.Data;
import tprog04.kremlin.aux_classes.MinistryEnum;

@Data
public class MinistryRequestDTO {
    private MinistryEnum name;
    private int purgeNr;
    private Set<String> actions;
}
