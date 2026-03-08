package tprog04.kremlin.mapper;

import org.springframework.stereotype.Component;
import tprog04.kremlin.dto.ministry.MinistryRequestDTO;
import tprog04.kremlin.dto.ministry.MinistryResponseDTO;
import tprog04.kremlin.models.Ministry;

@Component
public class MinistryMapper {

    public MinistryResponseDTO toDto(Ministry min) {
        MinistryResponseDTO dto = new MinistryResponseDTO();
        dto.setId(min.getId());
        dto.setName(min.getName());
        dto.setPurgeNr(min.getPurgeNr());

        return dto;
    }

    public Ministry toEntity(MinistryRequestDTO dto) {
        Ministry min = new Ministry();
        min.setName(dto.getName());
        min.setPurgeNr(dto.getPurgeNr());
        
        return min;
    }
}

