package tprog04.kremlin.dto.ministry;

import lombok.Data;
import tprog04.kremlin.aux_classes.MinistryEnum;

@Data
public class MinistryResponseDTO {
    private Long id;
    private MinistryEnum name;
    private int purgeNr;
}
