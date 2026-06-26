# Test Numerico Suma y Multiplicacion

Aplicacion Java/OpenXava para administrar y aplicar dos subpruebas numericas de la Bateria Factorial de Aptitudes (BFA):

- Numerico Sumas.
- Numerico Multiplicaciones.

El candidato responde con A/B:

- A = Verdadero.
- B = Falso.

Cada prueba tiene un limite de 4 minutos. Las respuestas se guardan en la base de datos, la calificacion se calcula en el servidor y el candidato solo ve un mensaje de registro final. Los resultados quedan disponibles para consulta administrativa.

## Tecnologias

- Java 17.
- Maven.
- OpenXava 7.7.
- PostgreSQL.
- Bootstrap 5 para la interfaz del candidato.
- Gson para la API JSON.
- BCrypt para cifrado de contrasenas.
- Lombok para getters/setters y constructores.

## Requisitos previos

Antes de ejecutar el proyecto, instalar:

1. JDK 17.
2. Maven 3.8 o superior.
3. PostgreSQL.
4. Un IDE recomendado: IntelliJ IDEA, Eclipse o NetBeans.

Verificar instalaciones:

```powershell
java -version
mvn -version
psql --version
```

## Configuracion de PostgreSQL

El proyecto esta configurado para usar PostgreSQL con la base `Estudiantedb`.

```text
Host: localhost
Puerto: 5432
Base de datos: Estudiantedb
Usuario: postgres
Contrasena: bd1234
```

La conexion esta en:

```text
src/main/webapp/META-INF/context.xml
```

Entrada actual:

```xml
<Resource name="jdbc/TestNumericoSumaMultiplicacionDS" auth="Container" type="javax.sql.DataSource"
      maxTotal="20" maxIdle="5" maxWaitMillis="10000"
      username="postgres" password="bd1234"
      driverClassName="org.postgresql.Driver"
      url="jdbc:postgresql://localhost:5432/Estudiantedb"/>
```

Para crear la base, tablas principales y preguntas, ejecutar este unico script:

```text
scripts/setup_postgres_bfa.sql
```

Comando:

```powershell
psql -U postgres -d postgres -f "scripts\setup_postgres_bfa.sql"
```

Cuando PostgreSQL pida contrasena, ingresar:

```text
bd1234
```

## Carga inicial de datos

El proyecto tiene un listener que carga datos automaticamente al arrancar:

```text
src/main/java/ni/edu/uam/TestNumericoSumaMultiplicacion/config/DatosInicialesListener.java
```

Ese listener crea datos cuando la base esta vacia. Si el script SQL ya creo los tests y preguntas, el listener asegura principalmente el usuario candidato demo.

El script SQL crea:

- Un registrador por defecto.
- Test Numerico Sumas.
- Test Numerico Multiplicaciones.
- 50 preguntas en total: 25 de sumas y 25 de multiplicaciones.

El listener crea:

- Un usuario candidato demo con contrasena cifrada.

Usuario demo para la pagina del candidato:

```text
Usuario: candidato
Contrasena: candidato123
```

Usuario administrativo OpenXava:

```text
Usuario: admin
Contrasena: admin
```

El usuario administrativo esta definido en:

```text
src/main/resources/naviox-users.properties
```

## Ejecutar el proyecto con Maven

Desde la raiz del proyecto:

```powershell
cd C:\ruta\al\proyecto\TestNumeroSumaMultiplicacion
```

Compilar:

```powershell
mvn -DskipTests package
```

Ejecutar:

```powershell
mvn exec:java
```

La clase principal es:

```text
ni.edu.uam.TestNumericoSumaMultiplicacion.run.TestNumericoSumaMultiplicacion
```

Tambien se puede ejecutar esa clase directamente desde el IDE.

## URLs principales

Cuando el servidor este iniciado, abrir:

```text
http://localhost:8080/TestNumericoSumaMultiplicacion/
```

Aplicacion administrativa OpenXava.

```text
http://localhost:8080/TestNumericoSumaMultiplicacion/app/
```

Aplicacion web para candidatos.

```text
http://localhost:8080/TestNumericoSumaMultiplicacion/api/tests
```

Endpoint para comprobar que la API responde.

## Flujo de uso

1. Iniciar PostgreSQL.
2. Ejecutar `psql -U postgres -d postgres -f "scripts\setup_postgres_bfa.sql"`.
3. Ejecutar `mvn -DskipTests package`.
4. Ejecutar `mvn exec:java`.
5. Al arrancar, `DatosInicialesListener` asegura el usuario demo `candidato / candidato123`.
6. Entrar a la aplicacion administrativa con `admin / admin`.
7. Entrar a la aplicacion del candidato con `candidato / candidato123`.
8. Seleccionar Sumas o Multiplicaciones.
9. Responder A/B.
10. Finalizar o esperar cierre por tiempo.
11. Revisar intentos y respuestas desde OpenXava.

## API REST

La API esta implementada en:

```text
src/main/java/ni/edu/uam/TestNumericoSumaMultiplicacion/api/ApiTestServlet.java
```

Rutas disponibles:

```text
POST /api/login
GET  /api/tests
GET  /api/tests/{id}/preguntas
POST /api/intentos/iniciar
POST /api/intentos/finalizar
GET  /api/intentos?idUsuario=N
```

La respuesta correcta no se envia al navegador. La calificacion se realiza en el servidor.

## Script de base de datos

El script completo esta en:

```text
scripts/setup_postgres_bfa.sql
```

Ejecutarlo conectado a la base `postgres`:

```powershell
psql -U postgres -d postgres -f "scripts\setup_postgres_bfa.sql"
```

Ese script crea la base `Estudiantedb`, crea las tablas principales si faltan e inserta el registrador, los tests y las 50 preguntas.

## Estructura importante

```text
src/main/java/ni/edu/uam/TestNumericoSumaMultiplicacion/modelo
```

Entidades principales: Usuario, Registrador, TestNumerico, Pregunta, IntentoTest y Respuesta.

```text
src/main/java/ni/edu/uam/TestNumericoSumaMultiplicacion/api
```

API JSON para la pagina del candidato.

```text
src/main/java/ni/edu/uam/TestNumericoSumaMultiplicacion/config
```

Carga inicial de datos.

```text
src/main/webapp/app
```

Interfaz web del candidato.

```text
src/main/webapp/META-INF/context.xml
```

Conexion a PostgreSQL.

```text
src/main/resources/naviox-users.properties
```

Usuario administrativo de OpenXava.

## Problemas comunes

### No reconoce `java`

Instalar JDK 17 y agregarlo al `PATH`.

### No reconoce `mvn`

Instalar Maven y agregarlo al `PATH`, o ejecutar el proyecto desde el IDE.

### Error de conexion a PostgreSQL

Revisar:

- Que PostgreSQL este encendido.
- Que exista la base `Estudiantedb`.
- Que el usuario sea `postgres`.
- Que la contrasena sea `bd1234`.
- Que el puerto sea `5432`.

### Puerto 8080 ocupado

Cerrar el proceso que usa el puerto 8080 o cambiar el puerto en la configuracion de OpenXava.

### No aparecen las preguntas

Verificar primero que la aplicacion haya arrancado sin errores. Luego revisar en OpenXava si existen registros en:

- Tests numericos.
- Preguntas.
- Usuarios.
- Registradores.

Si no existen, revisar errores de base de datos en la consola o ejecutar el SQL manual.

## Notas de seguridad

Las credenciales incluidas son para entorno academico/local. Antes de publicar el proyecto:

- Cambiar contrasenas.
- No subir credenciales reales.
- Restringir CORS si se despliega en produccion.
- Evitar depender de CDN si el entorno no tiene internet.

## Resumen del proyecto

Este sistema permite aplicar digitalmente los tests numericos de Sumas y Multiplicaciones de la BFA. El candidato trabaja contra reloj, responde A/B y el sistema guarda todo en PostgreSQL. La parte administrativa se maneja con OpenXava y la parte del candidato usa una interfaz web formal conectada a una API REST.
