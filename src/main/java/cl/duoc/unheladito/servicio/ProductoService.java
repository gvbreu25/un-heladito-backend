package cl.duoc.unheladito.servicio;

import cl.duoc.unheladito.dto.ProductoDatos;
import cl.duoc.unheladito.dto.ProductoVista;
import cl.duoc.unheladito.modelo.Categoria;
import cl.duoc.unheladito.modelo.Producto;
import cl.duoc.unheladito.repositorio.CategoriaRepository;
import cl.duoc.unheladito.repositorio.ProductoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ProductoService {
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final EntityManager entityManager;

    public ProductoService(ProductoRepository productos, CategoriaRepository categorias, EntityManager entityManager) {
        this.productos = productos;
        this.categorias = categorias;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<ProductoVista> listar() {
        return productos.findByActivoTrueOrderByIdAsc().stream().map(ProductoVista::de).toList();
    }

    @Transactional(readOnly = true)
    public ProductoVista buscar(String id) {
        return productos.findByIdAndActivoTrue(normalizar(id)).map(ProductoVista::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
    }

    @Transactional
    public ProductoVista crear(ProductoDatos datos) {
        if (datos.id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código del producto es obligatorio.");
        }
        String id = normalizar(datos.id());
        if (productos.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese código de producto ya está registrado.");
        }
        Producto producto = new Producto(id);
        copiarDatos(producto, datos);
        entityManager.persist(producto);
        entityManager.flush();
        return ProductoVista.de(producto);
    }

    @Transactional
    public ProductoVista actualizar(String id, ProductoDatos datos) {
        if (datos.id() != null && !normalizar(id).equals(normalizar(datos.id()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de un producto es fijo.");
        }
        Producto producto = buscarParaActualizar(id);
        copiarDatos(producto, datos);
        return ProductoVista.de(producto);
    }

    @Transactional
    public void eliminar(String id) {
        buscarParaActualizar(id).desactivar();
    }

    private Producto buscarParaActualizar(String id) {
        return productos.buscarParaActualizar(normalizar(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
    }

    private void copiarDatos(Producto producto, ProductoDatos datos) {
        Categoria categoria = categorias.findById(datos.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no existe."));
        producto.actualizar(datos.name().strip(), categoria, datos.price(), datos.stock(), datos.format().strip(),
                datos.allergens(), datos.description().strip(), datos.image(), datos.offer());
    }

    private String normalizar(String id) {
        return id.toUpperCase(Locale.ROOT);
    }
}
