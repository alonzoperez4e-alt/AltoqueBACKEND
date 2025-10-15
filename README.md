Proyecto AlToque - Backend
Este es el backend para el sistema de gestión de préstamos "AlToque", desarrollado con Spring Boot y Java 17.

Tecnologías Utilizadas
Java 17: Lenguaje de programación principal.

Spring Boot 3: Framework para la creación de la aplicación.

Spring Security: Para la gestión de autenticación y autorización con JWT.

JPA (Hibernate): Para el mapeo objeto-relacional y la persistencia de datos.

PostgreSQL: Base de datos relacional.

Maven: Gestor de dependencias y construcción del proyecto.

Configuración del Entorno
Para poder ejecutar el proyecto, es necesario configurar las variables de entorno que contienen las credenciales y secretos de la aplicación.

Crear el archivo .env:
En la raíz del proyecto, busca el archivo .env.example. Haz una copia de este archivo y renómbrala a .env.

Rellenar las variables:
Abre tu nuevo archivo .env y completa todas las variables con tus propias credenciales (base de datos, API, etc.).

SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=tu_usuario
SPRING_DATASOURCE_PASSWORD=tu_contraseña
API_PERU_TOKEN=tu_token_de_api
SPRING_MAIL_USERNAME=tu_correo@ejemplo.com
SPRING_MAIL_PASSWORD=tu_contraseña_de_aplicacion
JWT_SECRET=una_clave_secreta_muy_larga_y_segura
PORT=8080

Nota de Seguridad: Para SPRING_MAIL_PASSWORD con Gmail, se recomienda generar una "Contraseña de aplicación" desde la configuración de seguridad de tu cuenta de Google.

Ejecución
Una vez configurado el archivo .env, puedes compilar y ejecutar el proyecto usando Maven:

mvn spring-boot:run

El servidor se iniciará en el puerto definido en tu archivo .env (por defecto, el 8080).