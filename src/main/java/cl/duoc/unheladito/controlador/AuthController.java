package cl.duoc.unheladito.controlador;

import cl.duoc.unheladito.dto.UsuarioVista;
import cl.duoc.unheladito.servicio.AuthService;
import cl.duoc.unheladito.seguridad.SeguridadConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Sesión")
public class AuthController {
    private final AuthService servicio;
    private final boolean cookieSegura;

    public AuthController(AuthService servicio, @Value("${app.cookie.secure}") boolean cookieSegura) {
        this.servicio = servicio;
        this.cookieSegura = cookieSegura;
    }

    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Preparar la cookie antifalsificación para peticiones de escritura")
    public void csrf(CsrfToken token) {
        token.getToken();
    }

    @PostMapping("/registro")
    @Operation(summary = "Crear una cuenta de cliente e iniciar sesión")
    public ResponseEntity<UsuarioVista> registro(@Valid @RequestBody Registro datos) {
        return responder(HttpStatus.CREATED, servicio.registro(datos.name(), datos.email(), datos.password()));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y recibir el JWT en una cookie HttpOnly")
    public ResponseEntity<UsuarioVista> login(@Valid @RequestBody Login datos) {
        return responder(HttpStatus.OK, servicio.login(datos.email(), datos.password()));
    }

    @GetMapping("/me")
    @Operation(summary = "Recuperar el usuario autenticado y su rol")
    public ResponseEntity<UsuarioVista> perfil(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.perfil(jwt));
    }

    @PostMapping("/renovar")
    @Operation(summary = "Renovar un JWT vigente sin superar el límite de la sesión")
    public ResponseEntity<UsuarioVista> renovar(@AuthenticationPrincipal Jwt jwt) {
        return responder(HttpStatus.OK, servicio.renovar(jwt));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revocar la sesión y borrar la cookie")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        servicio.logout(jwt);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
    }

    private ResponseEntity<UsuarioVista> responder(HttpStatus estado, AuthService.Credenciales datos) {
        return ResponseEntity.status(estado).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie(datos.token(), datos.vigencia())).body(datos.usuario());
    }

    private String cookie(String token, Duration vigencia) {
        return ResponseCookie.from(SeguridadConfig.COOKIE_SESION, token)
                .httpOnly(true).secure(cookieSegura).sameSite("Strict").path("/api/v1")
                .maxAge(vigencia).build().toString();
    }

    public record Login(@NotBlank @Email @Size(max = 150) String email, @NotBlank @Size(max = 72) String password) {}
    public record Registro(@NotBlank @Size(min = 3, max = 100) String name,
                           @NotBlank @Email @Size(max = 150) String email,
                           @NotBlank @Size(min = 8, max = 72) String password) {}
}
