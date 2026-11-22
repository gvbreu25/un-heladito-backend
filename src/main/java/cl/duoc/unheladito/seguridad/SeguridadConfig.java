package cl.duoc.unheladito.seguridad;

import cl.duoc.unheladito.repositorio.SesionRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.*;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Configuration
public class SeguridadConfig {
    public static final String COOKIE_SESION = "heladito_sesion";
    public static final String EMISOR = "un-heladito";

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, JwtDecoder decoder,
                                @Value("${app.cookie.secure}") boolean cookieSegura) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(cookie -> cookie.sameSite("Strict").secure(cookieSegura).path("/"));

        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);

        // El JWT viaja en una cookie, por lo que las peticiones deben conservar la protección CSRF.
        var proveedor = new JwtAuthenticationProvider(decoder);
        proveedor.setJwtAuthenticationConverter(converter);
        var filtroJwt = new BearerTokenAuthenticationFilter(new ProviderManager(proveedor));
        filtroJwt.setBearerTokenResolver(request -> {
            String ruta = request.getServletPath();
            if (ruta.isEmpty()) ruta = request.getRequestURI();
            if (!ruta.startsWith("/api/v1/") || ruta.equals("/api/v1/auth/csrf")
                    || ruta.equals("/api/v1/auth/login") || ruta.equals("/api/v1/auth/registro")) {
                return null;
            }
            Cookie cookie = WebUtils.getCookie(request, COOKIE_SESION);
            return cookie == null ? null : cookie.getValue();
        });

        http.cors(Customizer.withDefaults())
                .logout(config -> config.disable())
                .csrf(config -> config.csrfTokenRepository(csrf)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(config -> config.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()))
                .addFilter(filtroJwt)
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf", "/api/v1/productos/**", "/api/v1/categorias").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro").permitAll()
                        .requestMatchers("/api/v1/auth/me", "/api/v1/auth/renovar", "/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/productos/**", "/api/v1/usuarios").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/mios").hasAnyRole("CLIENTE", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos").hasAnyRole("CLIENTE", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend.origin}") String origen) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origen));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey claveJwt(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("JWT_SECRET debe estar en Base64.", error);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe representar al menos 32 bytes aleatorios.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey clave, SesionRepository sesiones) {
        var decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> sesionActiva = jwt -> {
            try {
                String id = jwt.getClaimAsString("sid");
                Long usuario = Long.valueOf(jwt.getSubject());
                Instant ahora = Instant.now();
                if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isAfter(ahora) && id != null
                        && sesiones.existsByIdAndUsuarioIdAndVenceAfter(id, usuario, ahora)) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (IllegalArgumentException error) {
                // Un identificador de usuario inválido se trata como token rechazado.
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sesión inválida o vencida.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(EMISOR), sesionActiva));
        return decoder;
    }
}
