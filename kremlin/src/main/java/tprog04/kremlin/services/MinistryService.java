package tprog04.kremlin.services;

import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import tprog04.kremlin.dto.ministry.MinistryRequestDTO;
import tprog04.kremlin.dto.ministry.MinistryResponseDTO;
import tprog04.kremlin.mapper.MinistryMapper;
import tprog04.kremlin.models.Ministry;
import tprog04.kremlin.repositories.MinistryRepository;

@Service
public class MinistryService {

    @Autowired
    private MinistryRepository repoMinistry;
    @Autowired
    private MinistryMapper minMapper;

    public MinistryResponseDTO getMinistryById( Long id ) {
        Ministry found = this.repoMinistry.findById(id).get();
        return minMapper.toDto(found);
    }

    public List<MinistryResponseDTO> getAllMinistries() {
        return repoMinistry.findAll()
                           .stream()
                           .map(minMapper::toDto)
                           .collect(Collectors.toList());
    }

    public MinistryResponseDTO createSingleMinistry( MinistryRequestDTO dto ) {
        Ministry entity = minMapper.toEntity(dto);
        Ministry saved = repoMinistry.save(entity);
        return minMapper.toDto(saved);
    }

    public List<MinistryResponseDTO> createManyMinistries(List<MinistryRequestDTO> dtos)
    {
        List<Ministry> entities = dtos.stream()
                                      .map(minMapper::toEntity)
                                      .collect(Collectors.toList());
        
        List<Ministry> saved = repoMinistry.saveAll(entities);
        
        return saved.stream()
                    .map(minMapper::toDto)
                    .collect(Collectors.toList());
    }

    public List<MinistryResponseDTO> loadMinistries() throws IOException {

        if (this.repoMinistry.count() != 0) {
            throw new IllegalStateException("Ministerios ya cargados.\n");
        }
        
        List<Ministry> saved;

        String json = """
                        [
                            { "name" : "PARTY_CHIEF", "purgeNr" : 18 },
                            { "name" : "KGB_HERO", "purgeNr" : 14 },
                            { "name" : "FOREIGN", "purgeNr" : 14 },
                            { "name" : "DEFENSE", "purgeNr" : 14 },
                            { "name" : "IDEOLOGY", "purgeNr" : 10 },
                            { "name" : "INDUSTRY", "purgeNr" : 10 },
                            { "name" : "ECONOMY", "purgeNr" : 10 },
                            { "name" : "SPORTS", "purgeNr" : 10 },
                            { "name" : "CANDIDATE", "purgeNr" : 6 },
                            { "name" : "CANDIDATE", "purgeNr" : 6 },
                            { "name" : "CANDIDATE", "purgeNr" : 6 },
                            { "name" : "CANDIDATE", "purgeNr" : 6 },
                            { "name" : "CANDIDATE", "purgeNr" : 6 },
                            { "name" : "PEOPLE", "purgeNr" : null },
                            { "name" : "PEOPLE", "purgeNr" : null },
                            { "name" : "PEOPLE", "purgeNr" : null },
                            { "name" : "PEOPLE", "purgeNr" : null }
                        ]
                       """;
        
        ObjectMapper mapper = new ObjectMapper();
        List<Ministry> ministries = mapper.readValue(json, new TypeReference<List<Ministry>>() {});

        saved = this.repoMinistry.saveAll(ministries);
        
        return saved.stream()
                    .map(minMapper::toDto)
                    .collect(Collectors.toList());
    }

}
