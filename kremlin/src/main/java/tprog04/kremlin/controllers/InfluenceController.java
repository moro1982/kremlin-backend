package tprog04.kremlin.controllers;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tprog04.kremlin.dto.influence.assigned.AssignedRequestDTO;
import tprog04.kremlin.dto.influence.assigned.AssignedResponseDTO;
import tprog04.kremlin.dto.influence.declared.DeclaredRequestDTO;
import tprog04.kremlin.dto.influence.declared.DeclaredResponseDTO;
import tprog04.kremlin.mapper.InfluenceMapper;
import tprog04.kremlin.services.influence.implementations.AssignedInfluenceService;
import tprog04.kremlin.services.influence.implementations.DeclaredInfluenceService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/influence")
public class InfluenceController {

    @Autowired
    private AssignedInfluenceService assignedService;
    @Autowired
    private DeclaredInfluenceService declaredService;
    @Autowired
    private InfluenceMapper influenceMapper;
    
    /** En términos de la lógica del juego, la Influencia Asignada NO ES PÚBLICA **/
    /*
     * Estos métodos GET sólo se implementaron a los fines de probar la lógica de Asignación.
     * El acceso a los valores de Influencia Asignada sólo serán conocidos por el Jugador
     * que la asigna.
     * El resto de consultas a dichos registros se hará internamente, a fines de comparar
     * los valores de Influencia Declarada (o a declarar).
    */

    @GetMapping("/assigned/all")
    public List<AssignedResponseDTO> getAllAssigned() {
        return assignedService.getAllAssigned();
    }

    @GetMapping("/assigned/{id}")
    public AssignedResponseDTO getAssignedInfluenceById(@PathVariable("id") Long id) {
        return assignedService.getAssignedById(id);
    }

    @GetMapping("/assigned/player/{playerID}")
    public List<AssignedResponseDTO> getAssignedByPlayer(@PathVariable("playerID") Long playerID) {
        return assignedService.getAssignedByPlayer(playerID);
    }

    @GetMapping("/assigned/possibleValues/player/{playerID}")
    public List<Integer> getPossibleAssignValues(@PathVariable("playerID") Long playerID) {
        return assignedService.getPossibleValuesToAssign(playerID);
    }

    @PostMapping("/assigned")
    public AssignedResponseDTO assignInfluence(@RequestBody AssignedRequestDTO dto) {
        
        /* ASIGNACION */
        /* 
            Aquí será necesario hacer algunas validaciones importantes antes de guardar: 
         -> El total de Políticos a Influenciar x Jugador es 10 (ni más ni menos).
         -> Los puntos de Influencia Asignada x un mismo Jugador a sus 10 Politicos
            no pueden repetirse (o sea, 10 números, sin repetir, del 1 al 10).
         -> De aquí se desprende que el total de Influencia Asignada a los 10 Politicos
            suma siempre 55.
        */

        /* 
          Las validaciones las realizamos a través de un Servicio de Asignación
          y uno de Declaración.
        */

        // Attempting to assign influence
        AssignedResponseDTO responseDTO = this.assignedService.assignInfluence(dto);
        return responseDTO;
    }

    // Crear metodo de ruteo para declaracion
    @PostMapping("/declared")
    public DeclaredResponseDTO declareInfluence(@RequestBody DeclaredRequestDTO dto) {

        /* DECLARED */
        // If Declared on Politico <= Assigned on Politico
        // If Declared > Max (Declared Influences on Politico)

        // Attempting to declare influence
        DeclaredResponseDTO responseDTO = this.declaredService.declareInfluence(dto);
        return responseDTO;
    }

    @GetMapping("/declared/all")
    public List<DeclaredResponseDTO> getAllDeclared() {
        return declaredService.getAllDeclared()
                              .stream()
                              .map( declared -> influenceMapper.toDto(declared, new DeclaredResponseDTO()))
                              .collect(Collectors.toList());
    }

    @GetMapping("/declared/{id}")
    public DeclaredResponseDTO getDeclaredInfluenceById(@PathVariable("id") Long id) {
        return declaredService.getDeclaredById(id);
    }

    @GetMapping("/declared/player/{playerID}")
    public List<DeclaredResponseDTO> getDeclaredByPlayer(@PathVariable("playerID") Long playerID) {
        return declaredService.getDeclaredByPlayer(playerID);
    }

    @PostMapping("/declared/possibleValues")
    public List<Integer> getPossibleDeclareValues(@RequestBody DeclaredRequestDTO dto) {
        return this.declaredService.getPossibleValuesToDeclare(dto);
    }

}
