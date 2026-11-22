package cl.duoc.unheladito.controlador;

import cl.duoc.unheladito.dto.ProductoDatos;
import cl.duoc.unheladito.dto.ProductoVista;
import cl.duoc.unheladito.servicio.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos")
public class ProductoController {
    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar productos del catálogo")
    public List<ProductoVista> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un producto por código")
    public ProductoVista buscar(@PathVariable String id) {
        return servicio.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Crear un producto · Administrador")
    public ResponseEntity<ProductoVista> crear(@Valid @RequestBody ProductoDatos datos) {
        ProductoVista producto = servicio.crear(datos);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(producto.id()).toUri();
        return ResponseEntity.created(location).body(producto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un producto · Administrador")
    public ProductoVista actualizar(@PathVariable String id, @Valid @RequestBody ProductoDatos datos) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retirar un producto del catálogo · Administrador")
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
