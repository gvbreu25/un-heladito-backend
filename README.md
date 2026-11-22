# Un Heladito backend

API de la tienda Un Heladito para Desarrollo Full Stack II, DSY1104, sección 002D de Duoc UC.

## Ejecutar en Windows

1. Instala un **JDK 21** y **Apache Maven 3.9**. Comprueba `java -version` y `mvn -v` en PowerShell. Si usas IntelliJ puedes importar el archivo `pom.xml` y seleccionar el JDK 21 y el Maven del IDE.
2. En MySQL crea un esquema llamado `un_heladito` y una cuenta que pueda crear y modificar tablas en ese esquema. Configura el nombre y la contraseña de esa cuenta en la misma terminal donde iniciarás Spring.
3. Abre PowerShell en la carpeta que contiene `pom.xml`. Genera una clave nueva para JWT y configura la conexión.

```powershell
$bytesJwt = New-Object byte[] 32
$generadorJwt = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$generadorJwt.GetBytes($bytesJwt)
$generadorJwt.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytesJwt)
$env:DB_URL = 'jdbc:mysql://localhost:3306/un_heladito'
$env:DB_USER = Read-Host 'Usuario MySQL'
$env:DB_PASSWORD = [Net.NetworkCredential]::new('', (Read-Host 'Contraseña MySQL' -AsSecureString)).Password
```

4. Para la demostración, define contraseñas de prueba de al menos 8 caracteres. Los usuarios se crean al primer arranque que recibe su contraseña y la base guarda su hash BCrypt. Si la cuenta ya existe, configurar una contraseña distinta en el arranque no la cambia.

```powershell
$env:DEMO_ADMIN_PASSWORD = [Net.NetworkCredential]::new('', (Read-Host 'Clave de prueba del administrador' -AsSecureString)).Password
$env:DEMO_VENDOR_PASSWORD = [Net.NetworkCredential]::new('', (Read-Host 'Clave de prueba del vendedor' -AsSecureString)).Password
$env:DEMO_CLIENT_PASSWORD = [Net.NetworkCredential]::new('', (Read-Host 'Clave de prueba del cliente' -AsSecureString)).Password
mvn spring-boot:run
```

| Rol | Correo de prueba |
| --- | --- |
| Administrador | `admin@unheladito.test` |
| Vendedor | `ventas@unheladito.test` |
| Cliente | `cliente@unheladito.test` o una cuenta creada en la tienda |

Las contraseñas y la clave JWT se suministran como variables de entorno. Para comprobar la sesión tras reiniciar Spring, conserva la misma clave y base de datos. Java crea las tablas y carga una vez el catálogo base.

### Arranque local con H2

Si estás preparando la demostración sin MySQL, ejecuta los pasos de la clave JWT y las contraseñas de prueba, y luego:

```powershell
mvn "-Dspring-boot.run.profiles=local" spring-boot:run
```

Ese perfil usa H2 relacional en el archivo `data/un-heladito` para conservar los registros. Para demostrar el motor solicitado en el anexo, utiliza el arranque principal con MySQL.

## Swagger y pruebas

- API en `http://localhost:8080/api/v1`.
- Swagger en `http://localhost:8080/swagger-ui.html`.
- En Swagger ejecuta `GET /auth/csrf`, luego `POST /auth/login` con el usuario administrador. El navegador recibe la cookie JWT. Después ejecuta las operaciones de productos.
- Ejemplo de cuerpo de creación. Consulta antes `GET /api/v1/categorias` para elegir un `categoryId`.

```json
{
  "id": "DEMO01",
  "name": "Paleta de prueba",
  "categoryId": 1,
  "price": 2500,
  "stock": 8,
  "format": "Unidad",
  "description": "Paleta para la demostración de CRUD.",
  "offer": false
}
```

- `mvn clean test` ejecuta la prueba HTTP de integración con H2 en memoria. La prueba revisa catálogo, tres roles, pedidos, stock, CSRF, renovación y cierre de sesión. Las claves se generan dentro de la prueba.
- `FRONTEND_ORIGIN` vale `http://localhost:5173` por defecto. Si cambias la dirección de React, ajusta también esa variable. Para servir la aplicación por HTTPS, configura `COOKIE_SECURE=true` y usa las dos aplicaciones bajo el mismo sitio.

## Código y atribución

El caso y el catálogo provienen del proyecto EA2 de Eduardo Acevedo, Edward Cardoza y Gabriel Abreu. El backend y la integración corresponden a la continuación individual de Gabriel Abreu para la EA3. El repositorio original se conserva como referencia. https://github.com/EdACMDragonredgo/Un_heladito-
