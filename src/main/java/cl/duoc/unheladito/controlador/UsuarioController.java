package cl.duoc.unheladito.controlador;

import cl.duoc.unheladito.dto.UsuarioVista;
import cl.duoc.unheladito.repositorio.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios")
public class UsuarioController {
    private final UsuarioRepository usuarios;

    public UsuarioController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    @Operation(summary = "Consultar las cuentas registradas · Administrador")
    public List<UsuarioVista> listar() {
        return usuarios.findAllByOrderByIdAsc().stream().map(UsuarioVista::de).toList();
    }
}
