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
import tprog04.kremlin.dto.ministry.MinistryRequestDTO;
import tprog04.kremlin.dto.ministry.MinistryResponseDTO;
import tprog04.kremlin.services.MinistryService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/ministry")
public class MinistryController {
    
    @Autowired
    private MinistryService minService;

        /* Versión con DTO */

    @GetMapping("/{id}")
    public MinistryResponseDTO getMinistryById( @PathVariable("id") Long id) {
        return this.minService.getMinistryById(id);
    }

    @GetMapping("/all")
    public List<MinistryResponseDTO> getAllMinistries() {
        return this.minService.getAllMinistries();
    }

    @PostMapping("/single")
    public MinistryResponseDTO createSingleMinistry(@RequestBody MinistryRequestDTO dto)
    {
        return this.minService.createSingleMinistry(dto);
    }

    @PostMapping("/many")
    public List<MinistryResponseDTO> createManyMinistries
        (@RequestBody List<MinistryRequestDTO> dtos)
    {
        return this.minService.createManyMinistries(dtos);
    }

    @PostMapping("/loadAll")
    public List<MinistryResponseDTO> loadMinistries() throws IOException {
        return this.minService.loadMinistries();
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////
    
    /* Los siguientes 3 endpoints pertenecen ahora a GameMinistryController */

    // @PostMapping("/setup/assign_all_ministers")
    // public List<Ministry> assignInitialMinisters() {
    //     return this.minService.assignInitialMinisters();
    // }

    // @PutMapping("/{min}/minister/{pol}")
    // public MinistryResponseDTO assignMinister
    //     ( @PathVariable("min") Long min, @PathVariable("pol") Long pol )
    // {
    //     return this.minService.assignMinister(min, pol);
    // }

    /* ¿¿ Es @DeleteMapping o @PutMapping ?? */
    // @DeleteMapping("/{min}/minister")
    // public MinistryResponseDTO removeMinister(@PathVariable("min") Long min) {
    //     return this.minService.removeMinister(min);
    // }

////////////////////////////////////////////////////////////////////////////////////////////////

        /* Versión normal */

    // @GetMapping("/{id}")
    // public Ministry getMinistryById(@PathVariable("id") Long id)
    // {
    //     Ministry ministerio = this.repoMinisterio.findById(id).get();
    //     return ministerio;
    // }

    // @GetMapping("/all")
    // public List<Ministry> getMinisterios()
    // {
    //     return this.repoMinisterio.findAll();
    // }

    // @PostMapping()
    // public Ministry crearMinisterio(@RequestBody Ministry m)
    // {
    //     return this.repoMinisterio.save(m);
    // }

    // @PutMapping("/{min}/ministro/{pol}")
    // public Ministry asignarMinistro(
    //                                 @PathVariable("min") Long min,
    //                                 @PathVariable("pol") Long pol
    //                                )
    // {
    //     Ministry ministerio = repoMinisterio.findById(min).get();
    //     Politico nuevoMinistro = repoPolitico.findById(pol).get();
    //     ministerio.setMinister(nuevoMinistro);
    //     nuevoMinistro.setMinistry(ministerio);
    //     return repoMinisterio.save(ministerio);
    // }

    // @PutMapping("/{min}/acciones")
    // public Ministry asignarAcciones(
    //                                 @PathVariable("min") Long min,
    //                                 @RequestBody List<Long> actionIDs
    //                                )
    // {
    //     Ministry ministerio = repoMinisterio.findById(min).get();
    //     List<Action> acciones = repoAccion.findAllById(actionIDs);
    //     for (Action accion : acciones) {
    //         accion.getMinisterios().add(ministerio);
    //     }
    //     ministerio.getActions().addAll(acciones);
    //     return repoMinisterio.save(ministerio);
    // }

    // @DeleteMapping("/{min}/ministro")
    // public Ministry quitarMinistro(@PathVariable("min") Long min)
    // {
    //     Ministry ministerio = repoMinisterio.findById(min).get();
    //     Politico ministro = ministerio.getMinister();
    //     ministro.setMinistry(null);
    //     ministerio.setMinister(null);
    //     return repoMinisterio.save(ministerio);
    // }

    // @PatchMapping() ??

//////////////////////////////////////////////////////////////////////////////////////////



}
