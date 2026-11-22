package cl.duoc.unheladito;

import cl.duoc.unheladito.modelo.*;
import cl.duoc.unheladito.repositorio.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatosIniciales implements ApplicationRunner {
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final ObjectMapper json;
    private final PasswordEncoder contrasenas;
    private final Environment config;

    public DatosIniciales(CategoriaRepository categorias, ProductoRepository productos, UsuarioRepository usuarios,
                          ObjectMapper json, PasswordEncoder contrasenas, Environment config) {
        this.categorias = categorias;
        this.productos = productos;
        this.usuarios = usuarios;
        this.json = json;
        this.contrasenas = contrasenas;
        this.config = config;
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments args) throws IOException {
        if (categorias.count() == 0 && productos.count() == 0) cargarCatalogo();
        crearUsuario("Administración", "admin@unheladito.test", Rol.ADMINISTRADOR, "app.demo.admin-password");
        crearUsuario("Ventas", "ventas@unheladito.test", Rol.VENDEDOR, "app.demo.vendor-password");
        crearUsuario("Cliente de prueba", "cliente@unheladito.test", Rol.CLIENTE, "app.demo.client-password");
    }

    private void cargarCatalogo() throws IOException {
        List<ProductoInicial> datos;
        try (var archivo = new ClassPathResource("catalogo.json").getInputStream()) {
            datos = json.readValue(archivo, new TypeReference<List<ProductoInicial>>() {});
        }
        Map<String, Categoria> porNombre = new LinkedHashMap<>();
        for (ProductoInicial dato : datos) {
            Categoria categoria = porNombre.computeIfAbsent(dato.category(), nombre -> categorias.save(new Categoria(nombre)));
            Producto producto = new Producto(dato.id());
            producto.actualizar(dato.name(), categoria, dato.price(), dato.stock(), dato.format(), dato.allergens(),
                    dato.description(), dato.image(), dato.offer());
            productos.save(producto);
        }
    }

    private void crearUsuario(String nombre, String correo, Rol rol, String variable) {
        String password = config.getProperty(variable, "");
        if (password.isBlank() || usuarios.existsByCorreo(correo)) return;
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("La contraseña de prueba debe tener al menos 8 caracteres y hasta 72 bytes.");
        }
        usuarios.save(new Usuario(nombre, correo, contrasenas.encode(password), rol));
    }

    record ProductoInicial(String id, String name, String category, int price, String format,
                           int stock, String allergens, boolean offer, String description, String image) {}
}
