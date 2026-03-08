package tprog04.kremlin.controllers;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tprog04.kremlin.dto.politico.PoliticoRequestDTO;
import tprog04.kremlin.dto.politico.PoliticoResponseDTO;
import tprog04.kremlin.services.PoliticoService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/politico")
public class PoliticoController {
    
    @Autowired
    private PoliticoService polService;

    /* Versión con DTO */
    @GetMapping("/{id}")
    public PoliticoResponseDTO getPoliticoById(@PathVariable("id") Long id) {
        return this.polService.getPoliticoByID(id);
    }

    /* Versión con DTO */
    @GetMapping("/all")
    public List<PoliticoResponseDTO> getPoliticos() {
        return this.polService.getAllPoliticos();
    }

    /* Versión con DTO */
    @GetMapping("/name/{cadena}")
    public List<PoliticoResponseDTO> getPoliticosByName(@PathVariable("cadena") String cadena) {
        return this.polService.getPoliticosByName(cadena);
    }

    /* Versión con DTO */
    @PostMapping("/single")
    public PoliticoResponseDTO createSinglePolitico( @RequestBody PoliticoRequestDTO dto) {
        return this.polService.createSinglePolitico(dto);
    }

    /* Versión con DTO */
    @PostMapping("/many")
    public List<PoliticoResponseDTO> createManyPoliticos
        ( @RequestBody List<PoliticoRequestDTO> dtos )
    {
        return this.polService.createManyPoliticos(dtos);
    }

    /* Cargar directamente los Politicos desde el backend */
    @PostMapping("/loadAll")
    public List<PoliticoResponseDTO> loadPoliticos() throws IOException {
        return this.polService.loadPoliticos();
    }

    /* VERSIÓN NORMAL */

    // @GetMapping("/{id}")
    // public Politico getPoliticoById(@PathVariable("id") Long id) {
    //     Politico politico = this.repoPolitico.findById(id).get();
    //     return politico;
    // }

    // @GetMapping("/all")
    // public List<Politico> getPoliticos() {
    //     return repoPolitico.findAll();
    // }

    // @GetMapping("/name/{cadena}")
    // public List<Politico> getPoliticosByName(@PathVariable("cadena") String cadena) {
    //     return repoPolitico.findByNameLike(cadena);
    // }

    // @PostMapping("/single")
    // public Politico crearPolitico( @RequestBody Politico p ) {
    //     return repoPolitico.save(p);
    // }

    // @PostMapping("/many")
    // public List<Politico> crearPoliticos( @RequestBody List<Politico> politicos)
    // {
    //     return repoPolitico.saveAll(politicos);
    // }

}
