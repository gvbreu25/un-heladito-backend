package cl.duoc.unheladito.controlador;

import cl.duoc.unheladito.dto.PedidoDatos;
import cl.duoc.unheladito.dto.PedidoVista;
import cl.duoc.unheladito.servicio.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pedidos")
public class PedidoController {
    private final PedidoService servicio;

    public PedidoController(PedidoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar pedidos · Administrador y vendedor")
    public List<PedidoVista> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver detalle de un pedido · Administrador y vendedor")
    public PedidoVista buscar(@PathVariable Long id) {
        return servicio.buscar(id);
    }

    @GetMapping("/mios")
    @Operation(summary = "Consultar los pedidos de mi cuenta")
    public List<PedidoVista> listarPropios(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listarPropios(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un pedido y descontar el stock · Cliente o administrador")
    public PedidoVista registrar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PedidoDatos datos) {
        return servicio.registrar(Long.valueOf(jwt.getSubject()), datos);
    }
}
