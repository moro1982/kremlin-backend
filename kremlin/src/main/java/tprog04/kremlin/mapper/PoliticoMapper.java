package tprog04.kremlin.mapper;

import org.springframework.stereotype.Component;
import tprog04.kremlin.dto.politico.PoliticoRequestDTO;
import tprog04.kremlin.dto.politico.PoliticoResponseDTO;
import tprog04.kremlin.models.Politico;

@Component
public class PoliticoMapper {

    public PoliticoResponseDTO toDto(Politico politico) {
        PoliticoResponseDTO dto = new PoliticoResponseDTO();
        dto.setId(politico.getId());
        dto.setName(politico.getName());
        dto.setAlias(politico.getAlias());
        dto.setInitialAge(politico.getInitialAge());
        dto.setAdvantage(politico.getAdvantage());
        dto.setDisadvantage(politico.getDisadvantage());
        return dto;
    }

    public Politico toEntity(PoliticoRequestDTO dto) {
        Politico politico = new Politico();
        politico.setName(dto.getName());
        politico.setAlias(dto.getAlias());
        politico.setInitialAge(dto.getInitialAge());
        politico.setAdvantage(dto.getAdvantage());
        politico.setDisadvantage(dto.getDisadvantage());
        return politico;
    }
}
