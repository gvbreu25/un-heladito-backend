package cl.duoc.unheladito.servicio;

import cl.duoc.unheladito.dto.UsuarioVista;
import cl.duoc.unheladito.modelo.*;
import cl.duoc.unheladito.repositorio.*;
import cl.duoc.unheladito.seguridad.SeguridadConfig;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final Duration DURACION_TOKEN = Duration.ofMinutes(30);
    private static final Duration LIMITE_SESION = Duration.ofHours(8);

    private final UsuarioRepository usuarios;
    private final SesionRepository sesiones;
    private final PasswordEncoder contrasenas;
    private final JwtEncoder encoder;
    private final String hashSinUsuario;

    public AuthService(UsuarioRepository usuarios, SesionRepository sesiones,
                       PasswordEncoder contrasenas, JwtEncoder encoder) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.contrasenas = contrasenas;
        this.encoder = encoder;
        this.hashSinUsuario = contrasenas.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public Credenciales registro(String nombre, String correo, String password) {
        validarLongitud(password);
        String email = normalizar(correo);
        if (usuarios.existsByCorreo(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese correo ya está registrado.");
        }
        Usuario nuevo = usuarios.save(new Usuario(nombre.strip(), email, contrasenas.encode(password), Rol.CLIENTE));
        return abrirSesion(nuevo);
    }

    @Transactional
    public Credenciales login(String correo, String password) {
        validarLongitud(password);
        Usuario usuario = usuarios.findByCorreo(normalizar(correo)).orElse(null);
        String hash = usuario == null ? hashSinUsuario : usuario.getPasswordHash();
        if (!contrasenas.matches(password, hash) || usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }
        return abrirSesion(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioVista perfil(Jwt jwt) {
        return UsuarioVista.de(leerSesion(jwt).getUsuario());
    }

    @Transactional(readOnly = true)
    public Credenciales renovar(Jwt jwt) {
        return emitir(leerSesion(jwt));
    }

    @Transactional
    public void logout(Jwt jwt) {
        sesiones.deleteById(jwt.getClaimAsString("sid"));
    }

    private Sesion leerSesion(Jwt jwt) {
        return sesiones.findById(jwt.getClaimAsString("sid"))
                .filter(sesion -> sesion.getVence().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión vencida."));
    }

    private Credenciales abrirSesion(Usuario usuario) {
        Instant ahora = Instant.now();
        sesiones.deleteByVenceBefore(ahora);
        Sesion sesion = sesiones.save(new Sesion(UUID.randomUUID().toString(), usuario, ahora.plus(LIMITE_SESION)));
        return emitir(sesion);
    }

    private Credenciales emitir(Sesion sesion) {
        Usuario usuario = sesion.getUsuario();
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(DURACION_TOKEN);
        if (vence.isAfter(sesion.getVence())) vence = sesion.getVence();
        var claims = JwtClaimsSet.builder().issuer(SeguridadConfig.EMISOR)
                .subject(usuario.getId().toString()).issuedAt(ahora).expiresAt(vence)
                .claim("sid", sesion.getId()).claim("roles", List.of(usuario.getRol().name())).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims));
        return new Credenciales(UsuarioVista.de(usuario), token.getTokenValue(), Duration.between(ahora, vence));
    }

    private String normalizar(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }

    private void validarLongitud(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña supera el límite de 72 bytes.");
        }
    }

    public record Credenciales(UsuarioVista usuario, String token, Duration vigencia) {}
}
