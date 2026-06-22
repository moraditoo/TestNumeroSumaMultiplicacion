package ni.edu.uam.TestNumericoSumaMultiplicacion.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openxava.jpa.XPersistence;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.EstadoIntento;
import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.IntentoTest;
import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.Pregunta;
import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.Respuesta;
import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.TestNumerico;
import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.Usuario;
import ni.edu.uam.TestNumericoSumaMultiplicacion.config.DatosInicialesListener;
import ni.edu.uam.TestNumericoSumaMultiplicacion.util.PasswordUtil;

/**
 * API REST (JSON) que sirve de puente entre la pagina web de los usuarios y los
 * datos administrados en OpenXava. Comparte la misma base de datos PostgreSQL.
 *
 * Rutas (todas bajo /api):
 *   POST /api/login                 -> autentica un Usuario (usuario o correo + contrasena)
 *   GET  /api/tests                 -> lista de test disponibles (con preguntas activas)
 *   GET  /api/tests/{id}/preguntas  -> preguntas activas de un test (SIN la respuesta correcta)
 *   POST /api/intentos              -> guarda un intento, califica en el servidor y devuelve el resultado
 *   GET  /api/intentos?idUsuario=N  -> historial de intentos de un usuario (gestiones)
 *
 * La respuesta correcta nunca se envia al navegador: la calificacion se hace aqui.
 */
public class ApiTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();

    // ------------------------------------------------------------------ CORS
    private void cors(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        cors(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    // ------------------------------------------------------------------- GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        cors(resp);
        DatosInicialesListener.sembrarSiVacio();
        String ruta = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            if (ruta.equals("/tests")) {
                listarTests(resp);
            } else if (ruta.matches("/tests/\\d+/preguntas")) {
                long idTest = Long.parseLong(ruta.split("/")[2]);
                listarPreguntas(idTest, resp);
            } else if (ruta.equals("/intentos")) {
                historialIntentos(req, resp);
            } else {
                error(resp, 404, "Ruta no encontrada: " + ruta);
            }
            XPersistence.commit();
        } catch (Exception ex) {
            XPersistence.rollback();
            error(resp, 500, "Error del servidor: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        cors(resp);
        DatosInicialesListener.sembrarSiVacio();
        String ruta = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            String cuerpo = req.getReader().lines().collect(Collectors.joining());
            JsonObject json = cuerpo.isBlank()
                    ? new JsonObject()
                    : JsonParser.parseString(cuerpo).getAsJsonObject();

            if (ruta.equals("/login")) {
                login(json, resp);
            } else if (ruta.equals("/intentos")) {
                guardarIntento(json, resp);
            } else {
                error(resp, 404, "Ruta no encontrada: " + ruta);
            }
            XPersistence.commit();
        } catch (Exception ex) {
            XPersistence.rollback();
            error(resp, 500, "Error del servidor: " + ex.getMessage());
        }
    }

    // ============================================================ OPERACIONES

    /** Autentica un usuario por nombre de usuario o correo y verifica la contrasena cifrada. */
    private void login(JsonObject json, HttpServletResponse resp) throws IOException {
        String identificador = texto(json, "usuario");
        String contrasena = texto(json, "contrasena");

        if (identificador.isBlank() || contrasena.isBlank()) {
            error(resp, 400, "Debe indicar usuario y contrasena.");
            return;
        }

        EntityManager em = XPersistence.getManager();
        Usuario usuario;
        try {
            usuario = em.createQuery(
                    "from Usuario u where u.nombreUsuario = :id or u.correo = :id", Usuario.class)
                    .setParameter("id", identificador)
                    .getSingleResult();
        } catch (NoResultException e) {
            error(resp, 401, "Usuario o contrasena incorrectos.");
            return;
        }

        if (!usuario.isActivo()) {
            error(resp, 403, "El usuario esta inactivo. Contacte al administrador.");
            return;
        }
        if (!PasswordUtil.coincide(contrasena, usuario.getContrasena())) {
            error(resp, 401, "Usuario o contrasena incorrectos.");
            return;
        }

        JsonObject salida = new JsonObject();
        salida.addProperty("idUsuario", usuario.getIdUsuario());
        salida.addProperty("nombres", usuario.getNombres());
        salida.addProperty("apellidos", usuario.getApellidos());
        salida.addProperty("nombreUsuario", usuario.getNombreUsuario());
        salida.addProperty("correo", usuario.getCorreo());
        escribir(resp, 200, salida);
    }

    /** Lista los test que estan activos y tienen al menos una pregunta activa. */
    private void listarTests(HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();
        List<TestNumerico> tests = em.createQuery(
                "from TestNumerico t where t.activo = true order by t.tipoTest", TestNumerico.class)
                .getResultList();

        JsonArray arreglo = new JsonArray();
        for (TestNumerico t : tests) {
            long activas = em.createQuery(
                    "select count(p) from Pregunta p where p.testNumerico = :t and p.activa = true", Long.class)
                    .setParameter("t", t)
                    .getSingleResult();
            if (activas == 0) {
                continue;
            }
            JsonObject o = new JsonObject();
            o.addProperty("idTestNumerico", t.getIdTestNumerico());
            o.addProperty("nombre", t.getNombre());
            o.addProperty("descripcion", t.getDescripcion());
            o.addProperty("tipoTest", t.getTipoTest());
            o.addProperty("cantidadPreguntas", activas);
            o.addProperty("tiempoLimiteMinutos", TestNumerico.TIEMPO_LIMITE_MINUTOS);
            arreglo.add(o);
        }
        escribir(resp, 200, arreglo);
    }

    /** Devuelve las preguntas activas de un test, SIN exponer la respuesta correcta. */
    private void listarPreguntas(long idTest, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();
        List<Pregunta> preguntas = em.createQuery(
                "from Pregunta p where p.testNumerico.idTestNumerico = :id and p.activa = true "
                + "order by p.numeroPregunta", Pregunta.class)
                .setParameter("id", idTest)
                .getResultList();

        JsonArray arreglo = new JsonArray();
        for (Pregunta p : preguntas) {
            JsonObject o = new JsonObject();
            o.addProperty("idPregunta", p.getIdPregunta());
            o.addProperty("numeroPregunta", p.getNumeroPregunta());
            o.addProperty("operacion", p.getOperacion());
            o.addProperty("resultadoMostrado", p.getResultadoMostrado());
            arreglo.add(o);
        }
        escribir(resp, 200, arreglo);
    }

    /** Crea y califica un intento. Espera {idUsuario, idTestNumerico, cerradoPorTiempo, respuestas:[{idPregunta, respuestaMarcada}]}. */
    private void guardarIntento(JsonObject json, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();

        Long idUsuario = numero(json, "idUsuario");
        Long idTest = numero(json, "idTestNumerico");
        boolean cerradoPorTiempo = json.has("cerradoPorTiempo") && json.get("cerradoPorTiempo").getAsBoolean();

        if (idUsuario == null || idTest == null) {
            error(resp, 400, "Faltan datos del intento (usuario o test).");
            return;
        }

        Usuario usuario = em.find(Usuario.class, idUsuario);
        TestNumerico test = em.find(TestNumerico.class, idTest);
        if (usuario == null || test == null) {
            error(resp, 404, "Usuario o test inexistente.");
            return;
        }

        IntentoTest intento = test.iniciarTest(usuario);

        List<JsonObject> entradas = new ArrayList<>();
        if (json.has("respuestas") && json.get("respuestas").isJsonArray()) {
            for (var elem : json.getAsJsonArray("respuestas")) {
                entradas.add(elem.getAsJsonObject());
            }
        }

        for (JsonObject entrada : entradas) {
            Long idPregunta = numero(entrada, "idPregunta");
            String marcada = texto(entrada, "respuestaMarcada");
            Pregunta pregunta = idPregunta == null ? null : em.find(Pregunta.class, idPregunta);
            if (pregunta == null) {
                continue;
            }
            Respuesta r = new Respuesta();
            r.setPregunta(pregunta);
            r.setRespuestaMarcada(marcada == null ? null : marcada.trim().toUpperCase());
            intento.registrarRespuesta(r);
        }

        if (cerradoPorTiempo) {
            intento.cerrarPorTiempo();
        } else {
            intento.finalizar();
        }
        intento.calcularResultado();

        em.persist(intento);

        JsonObject salida = new JsonObject();
        salida.addProperty("idIntentoTest", intento.getIdIntentoTest());
        salida.addProperty("aciertos", intento.getAciertos());
        salida.addProperty("errores", intento.getErrores());
        salida.addProperty("sinResponder", intento.getSinResponder());
        salida.addProperty("cantidadPreguntas", intento.getCantidadPreguntas());
        salida.addProperty("estado", intento.getEstado().name());
        salida.addProperty("tiempoUsadoSegundos", intento.calcularTiempoUsado());
        salida.addProperty("mensaje", intento.generarMensajeFinal());
        escribir(resp, 200, salida);
    }

    /** Historial de intentos de un usuario (sus gestiones). */
    private void historialIntentos(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idParam = req.getParameter("idUsuario");
        if (idParam == null || idParam.isBlank()) {
            error(resp, 400, "Falta el parametro idUsuario.");
            return;
        }
        long idUsuario = Long.parseLong(idParam);

        EntityManager em = XPersistence.getManager();
        List<IntentoTest> intentos = em.createQuery(
                "from IntentoTest i where i.usuario.idUsuario = :id order by i.fechaInicio desc", IntentoTest.class)
                .setParameter("id", idUsuario)
                .getResultList();

        JsonArray arreglo = new JsonArray();
        for (IntentoTest i : intentos) {
            JsonObject o = new JsonObject();
            o.addProperty("idIntentoTest", i.getIdIntentoTest());
            o.addProperty("test", i.getTestNumerico() != null ? i.getTestNumerico().getNombre() : "");
            o.addProperty("tipoTest", i.getTestNumerico() != null ? i.getTestNumerico().getTipoTest() : "");
            o.addProperty("fechaInicio", i.getFechaInicio() != null ? i.getFechaInicio().toString() : "");
            o.addProperty("aciertos", i.getAciertos());
            o.addProperty("errores", i.getErrores());
            o.addProperty("sinResponder", i.getSinResponder());
            o.addProperty("cantidadPreguntas", i.getCantidadPreguntas());
            o.addProperty("estado", i.getEstado() != null ? i.getEstado().name() : "");
            o.addProperty("tiempoUsadoSegundos", i.calcularTiempoUsado());
            arreglo.add(o);
        }
        escribir(resp, 200, arreglo);
    }

    // ================================================================ HELPERS

    private String texto(JsonObject json, String clave) {
        return json.has(clave) && !json.get(clave).isJsonNull() ? json.get(clave).getAsString() : "";
    }

    private Long numero(JsonObject json, String clave) {
        try {
            return json.has(clave) && !json.get(clave).isJsonNull() ? json.get(clave).getAsLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void escribir(HttpServletResponse resp, int codigo, Object cuerpo) throws IOException {
        resp.setStatus(codigo);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(cuerpo));
        }
    }

    private void error(HttpServletResponse resp, int codigo, String mensaje) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("error", mensaje);
        escribir(resp, codigo, o);
    }
}
