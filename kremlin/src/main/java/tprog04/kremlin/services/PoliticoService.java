package tprog04.kremlin.services;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import tprog04.kremlin.dto.politico.PoliticoRequestDTO;
import tprog04.kremlin.dto.politico.PoliticoResponseDTO;
import tprog04.kremlin.mapper.PoliticoMapper;
import tprog04.kremlin.models.Politico;
import tprog04.kremlin.repositories.PoliticoRepository;

@Service
public class PoliticoService {

    @Autowired
    private PoliticoRepository repoPolitico;
    @Autowired
    private PoliticoMapper polMapper;

    public PoliticoResponseDTO getPoliticoByID(Long id) {
      Politico found = this.repoPolitico.findById(id).get();
      return polMapper.toDto(found);
    }

    public List<PoliticoResponseDTO> getAllPoliticos() {
      return repoPolitico.findAll()
                           .stream()
                           .map(polMapper::toDto)
                           .collect(Collectors.toList());
    }

    public List<PoliticoResponseDTO> getPoliticosByName(String cadena) {
        return repoPolitico.findByNameLike(cadena)
                           .stream()
                           .map(polMapper::toDto)
                           .collect(Collectors.toList());
    }

    public PoliticoResponseDTO createSinglePolitico(PoliticoRequestDTO dto) {
        Politico entity = polMapper.toEntity(dto);
        Politico saved = repoPolitico.save(entity);
        return polMapper.toDto(saved);
    }

    public List<PoliticoResponseDTO> createManyPoliticos( List<PoliticoRequestDTO> dtos )
    {
        List<Politico> entities = dtos.stream()
                                       .map(polMapper::toEntity)
                                       .collect(Collectors.toList());
        
        List<Politico> saved = repoPolitico.saveAll(entities);
        
        return saved.stream()
                    .map(polMapper::toDto)
                    .collect(Collectors.toList());
    }

    public List<PoliticoResponseDTO> loadPoliticos() throws IOException {
        
        if (this.repoPolitico.count() != 0) {
          throw new IllegalStateException("Politicos already loaded.\n");
        }

        List<Politico> saved;

        String json = """
          [
            { "name": "Konstantin Chernenko", "alias": "A", "initialAge": 70, 
                      "advantage" : "IDEOLOGY", "disadvantage" : "PARTY_CHIEF" },
            { "name": "Andrei Gromyko", "alias": "B", "initialAge": 69, 
                      "advantage" : "FOREIGN", "disadvantage" : "INDUSTRY" },
            { "name": "Mikhail Suslov", "alias": "C", "initialAge": 68, 
                      "advantage" : "IDEOLOGY", "disadvantage" : "SPORTS" },
            { "name": "Yuri Andropov", "alias": "D", "initialAge": 66, 
                      "advantage" : "DEFENSE", "disadvantage" : null },
            { "name": "Alexei Kosygin", "alias": "E", "initialAge": 65, 
                      "advantage" : "ECONOMY", "disadvantage" : "IDEOLOGY" },
            { "name": "Leonid Brezhnev", "alias": "F", "initialAge": 64, 
                      "advantage" : "FOREIGN", "disadvantage" : "ECONOMY" },
            { "name": "Nikolay Kovalyov", "alias": "G", "initialAge": 63, 
                      "advantage" : "KGB_HERO", "disadvantage" : "FOREIGN" },
            { "name": "Yevgeny Primakov", "alias": "H", "initialAge": 67, 
                      "advantage" : "INDUSTRY", "disadvantage" : null },
            { "name": "Yevgeny Shaposhnikov", "alias": "I", "initialAge": 66, 
                      "advantage" : "DEFENSE", "disadvantage" : "KGB_HERO" },
            { "name": "Boris Yeltsin", "alias": "J", "initialAge": 62, 
                      "advantage" : "ECONOMY", "disadvantage" : "DEFENSE" },
            { "name": "Alexander Bortnikov", "alias": "K", "initialAge": 61, 
                      "advantage" : "KGB_HERO", "disadvantage" : "SPORTS" },
            { "name": "Nikolay Patrushev", "alias": "L", "initialAge": 61, 
                      "advantage" : "KGB_HERO", "disadvantage" : "DEFENSE" },
            { "name": "Vladimir Putin", "alias": "M", "initialAge": 60, 
                      "advantage" : "PARTY_CHIEF", "disadvantage" : "FOREIGN" },
            { "name": "Sergei Stepashin", "alias": "N", "initialAge": 59, 
                      "advantage" : "ECONOMY", "disadvantage" : "FOREIGN" },
            { "name": "Viktor Zubkov", "alias": "O", "initialAge": 58, 
                      "advantage" : "INDUSTRY", "disadvantage" : "KGB_HERO" },
            { "name": "Vladimir Petukhov", "alias": "P", "initialAge": 57, 
                      "advantage" : "INDUSTRY", "disadvantage" : "ECONOMY" },
            { "name": "Gennady Zyuganov", "alias": "Q", "initialAge": 56, 
                      "advantage" : "IDEOLOGY", "disadvantage" : "INDUSTRY" },
            { "name": "Valentin Pavlov", "alias": "R", "initialAge": 53, 
                      "advantage" : "ECONOMY", "disadvantage" : "DEFENSE" },
            { "name": "Anatoliy Serdyukov", "alias": "S", "initialAge": 51, 
                      "advantage" : "DEFENSE", "disadvantage" : "ECONOMY" },
            { "name": "Mikhail Khodorkovsky", "alias": "T", "initialAge": 50, 
                      "advantage" : "ECONOMY", "disadvantage" : "IDEOLOGY" },
            { "name": "Mikhail Gorbachev", "alias": "U", "initialAge": 49, 
                      "advantage" : "FOREIGN", "disadvantage" : "INDUSTRY" },
            { "name": "Sergei Kiriyenko", "alias": "V", "initialAge": 48, 
                      "advantage" : "INDUSTRY", "disadvantage" : "DEFENSE" },
            { "name": "Dmitri Medvedev", "alias": "W", "initialAge": 47, 
                      "advantage" : "FOREIGN", "disadvantage" : "PARTY_CHIEF" },
            { "name": "Roman Abramovich", "alias": "X", "initialAge": 46, 
                      "advantage" : "SPORTS", "disadvantage" : "ECONOMY" },
            { "name": "Boris Berezovsky", "alias": "Y", "initialAge": 43, 
                      "advantage" : "INDUSTRY", "disadvantage" : "IDEOLOGY" },
            { "name": "Viktor Chernomyrdin", "alias": "Z", "initialAge": 42, 
                      "advantage" : "SPORTS", "disadvantage" : "KGB_HERO" }
          ]
        """;
        
        ObjectMapper mapper = new ObjectMapper();
        List<Politico> politicos = mapper.readValue(json, new TypeReference<List<Politico>>() {});
        
        saved = this.repoPolitico.saveAll(politicos);
        
        return saved.stream()
                    .map(polMapper::toDto)
                    .collect(Collectors.toList());
    }
    
}
