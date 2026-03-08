package tprog04.kremlin.dto.politico;

import lombok.Data;
import tprog04.kremlin.aux_classes.MinistryEnum;

@Data
public class PoliticoResponseDTO {
    private Long id;
    private String name;
    private String alias;
    private int initialAge;
    private MinistryEnum advantage;
    private MinistryEnum disadvantage;
}
