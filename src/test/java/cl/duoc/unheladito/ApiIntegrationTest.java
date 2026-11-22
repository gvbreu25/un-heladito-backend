package cl.duoc.unheladito;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:heladito-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    private static final String API = "/api/v1";
    private static final String PASSWORD = UUID.randomUUID().toString();

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry props) {
        byte[] secreto = new byte[32];
        new SecureRandom().nextBytes(secreto);
        String clave = Base64.getEncoder().encodeToString(secreto);
        props.add("app.jwt.secret", () -> clave);
        props.add("app.demo.admin-password", () -> PASSWORD);
        props.add("app.demo.vendor-password", () -> PASSWORD);
    }

    @Test
    void integraCatalogoPedidosYLosTresRoles() throws Exception {
        Cookie antixsrf = mvc.perform(get(API + "/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        String csrf = antixsrf.getValue();
        Cookie admin = login(antixsrf, "admin@unheladito.test", "ADMINISTRADOR");
        Cookie vendedor = login(antixsrf, "ventas@unheladito.test", "VENDEDOR");
        mvc.perform(get(API + "/auth/me").cookie(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMINISTRADOR"));

        Cookie cliente = mvc.perform(post(API + "/auth/registro").cookie(antixsrf).header("X-XSRF-TOKEN", csrf)
                        .contentType(APPLICATION_JSON).content("""
                                {"name":"Persona de prueba","email":"persona@unheladito.test","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CLIENTE"))
                .andReturn().getResponse().getCookie("heladito_sesion");

        String nuevo = """
                {"id":"PRUEBA-CREAR","name":"Sabor de prueba","categoryId":1,"price":2500,"stock":3,
                 "format":"Unidad","description":"Producto temporal de la prueba","offer":false}
                """;
        mvc.perform(post(API + "/productos").cookie(admin).contentType(APPLICATION_JSON).content(nuevo))
                .andExpect(status().isForbidden());
        mvc.perform(post(API + "/productos").cookie(antixsrf, vendedor).header("X-XSRF-TOKEN", csrf)
                .contentType(APPLICATION_JSON).content(nuevo)).andExpect(status().isForbidden());
        mvc.perform(post(API + "/productos").cookie(antixsrf, admin).header("X-XSRF-TOKEN", csrf)
                .contentType(APPLICATION_JSON).content(nuevo)).andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/productos/PRUEBA-CREAR")));
        mvc.perform(get(API + "/productos/PRUEBA-CREAR")).andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(2500));
        mvc.perform(put(API + "/productos/PRUEBA-CREAR").cookie(antixsrf, admin).header("X-XSRF-TOKEN", csrf)
                .contentType(APPLICATION_JSON).content(nuevo.replace("2500", "2900")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(2900));

        String pedido = """
                {"name":"Persona de prueba","email":"persona@unheladito.test","street":"Los Álamos 123",
                 "region":"Región Metropolitana","commune":"Melipilla","delivery":"Domicilio estándar",
                 "items":[{"productId":"PRUEBA-CREAR","quantity":2}]}
                """;
        mvc.perform(post(API + "/pedidos").cookie(antixsrf, cliente).header("X-XSRF-TOKEN", csrf)
                .contentType(APPLICATION_JSON).content(pedido)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(5800));
        mvc.perform(post(API + "/pedidos").cookie(antixsrf, cliente).header("X-XSRF-TOKEN", csrf)
                .contentType(APPLICATION_JSON).content(pedido)).andExpect(status().isConflict());
        mvc.perform(get(API + "/productos/PRUEBA-CREAR")).andExpect(jsonPath("$.stock").value(1));

        mvc.perform(get(API + "/pedidos").cookie(vendedor)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].price").value(2900));
        mvc.perform(get(API + "/pedidos").cookie(cliente)).andExpect(status().isForbidden());
        mvc.perform(get(API + "/pedidos/mios").cookie(cliente)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].qty").value(2));
        mvc.perform(get(API + "/usuarios").cookie(vendedor)).andExpect(status().isForbidden());

        mvc.perform(delete(API + "/productos/PRUEBA-CREAR").cookie(antixsrf, admin).header("X-XSRF-TOKEN", csrf))
                .andExpect(status().isNoContent());
        mvc.perform(get(API + "/productos/PRUEBA-CREAR")).andExpect(status().isNotFound());
        mvc.perform(get(API + "/pedidos/mios").cookie(cliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].items[0].id").value("PRUEBA-CREAR"));

        Cookie renovada = mvc.perform(post(API + "/auth/renovar").cookie(antixsrf, admin).header("X-XSRF-TOKEN", csrf))
                .andExpect(status().isOk()).andExpect(cookie().httpOnly("heladito_sesion", true))
                .andReturn().getResponse().getCookie("heladito_sesion");
        mvc.perform(post(API + "/auth/logout").cookie(antixsrf, renovada).header("X-XSRF-TOKEN", csrf))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge("heladito_sesion", 0));
        mvc.perform(get(API + "/auth/me").cookie(renovada)).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/productos'].post").exists());
    }

    private Cookie login(Cookie csrf, String email, String role) throws Exception {
        return mvc.perform(post(API + "/auth/login").cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(APPLICATION_JSON).content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role))
                .andExpect(cookie().httpOnly("heladito_sesion", true))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andReturn().getResponse().getCookie("heladito_sesion");
    }
}
