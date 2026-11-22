package cl.duoc.unheladito.controlador;

import cl.duoc.unheladito.modelo.Categoria;
import cl.duoc.unheladito.repositorio.CategoriaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías")
public class CategoriaController {
    private final CategoriaRepository categorias;

    public CategoriaController(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    @GetMapping
    @Operation(summary = "Listar categorías disponibles")
    public List<CategoriaVista> listar() {
        return categorias.findAllByOrderByIdAsc().stream().map(CategoriaVista::de).toList();
    }

    public record CategoriaVista(Long id, String name) {
        static CategoriaVista de(Categoria dato) {
            return new CategoriaVista(dato.getId(), dato.getNombre());
        }
    }
}
