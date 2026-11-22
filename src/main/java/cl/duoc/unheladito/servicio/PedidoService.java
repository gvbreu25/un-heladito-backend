package cl.duoc.unheladito.servicio;

import cl.duoc.unheladito.dto.PedidoDatos;
import cl.duoc.unheladito.dto.PedidoVista;
import cl.duoc.unheladito.modelo.Boleta;
import cl.duoc.unheladito.modelo.Producto;
import cl.duoc.unheladito.modelo.Usuario;
import cl.duoc.unheladito.repositorio.BoletaRepository;
import cl.duoc.unheladito.repositorio.ProductoRepository;
import cl.duoc.unheladito.repositorio.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class PedidoService {
    private final BoletaRepository boletas;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;

    public PedidoService(BoletaRepository boletas, ProductoRepository productos, UsuarioRepository usuarios) {
        this.boletas = boletas;
        this.productos = productos;
        this.usuarios = usuarios;
    }

    @Transactional
    public PedidoVista registrar(Long usuarioId, PedidoDatos datos) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow();
        Boleta boleta = new Boleta(usuario, datos.name(), datos.email(), datos.street(), datos.commune(),
                datos.region(), datos.delivery(), datos.notes());

        // Agrupa artículos repetidos y bloquea los productos siempre en el mismo orden.
        Map<String, Integer> cantidades = new TreeMap<>();
        for (PedidoDatos.Item item : datos.items()) {
            cantidades.merge(item.productId().toUpperCase(Locale.ROOT), item.quantity(), Integer::sum);
        }
        for (var linea : cantidades.entrySet()) {
            Producto producto = productos.buscarParaActualizar(linea.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Producto no disponible: " + linea.getKey()));
            int cantidad = linea.getValue();
            if (cantidad > producto.getStock()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente de " + producto.getNombre() + ".");
            }
            boleta.agregarDetalle(producto, cantidad);
            producto.descontarStock(cantidad);
        }
        return PedidoVista.de(boletas.save(boleta));
    }

    @Transactional(readOnly = true)
    public List<PedidoVista> listar() {
        return boletas.findAllByOrderByFechaDesc().stream().map(PedidoVista::de).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoVista> listarPropios(Long usuarioId) {
        return boletas.findByUsuarioIdOrderByFechaDesc(usuarioId).stream().map(PedidoVista::de).toList();
    }

    @Transactional(readOnly = true)
    public PedidoVista buscar(Long id) {
        return boletas.findById(id).map(PedidoVista::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
    }
}
