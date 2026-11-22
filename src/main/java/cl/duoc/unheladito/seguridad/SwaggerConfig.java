package cl.duoc.unheladito.seguridad;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Un Heladito API", version = "v1",
        description = "Usa GET /api/v1/auth/csrf antes de la primera escritura. Inicia sesión con POST /api/v1/auth/login. "
                + "El navegador envía la cookie de sesión automáticamente durante las pruebas en Swagger."))
public class SwaggerConfig {}
