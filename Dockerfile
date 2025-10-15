# Imagen base de Java 17 (compatible con Spring Boot)
FROM openjdk:17-jdk-slim

# Copiar el archivo JAR generado por Maven
COPY target/*.jar app.jar

# Exponer el puerto que usa Spring Boot
EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "/app.jar"]
