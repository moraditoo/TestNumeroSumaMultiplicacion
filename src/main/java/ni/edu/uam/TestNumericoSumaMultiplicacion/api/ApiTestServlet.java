package ni.edu.uam.TestNumericoSumaMultiplicacion.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 *   POST /api/intentos/iniciar      -> crea el intento, arranca el tiempo y devuelve preguntas
 *   POST /api/intentos/finalizar    -> finaliza, califica y guarda respuestas en el servidor
 *   GET  /api/intentos?idUsuario=N  -> historial publico de intentos del usuario
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
            } else if (ruta.equals("/intentos/iniciar")) {
                iniciarIntento(json, resp);
            } else if (ruta.equals("/intentos/finalizar")) {
                finalizarIntento(json, resp);
            } else if (ruta.equals("/intentos")) {
                error(resp, 410, "Use /intentos/iniciar y /intentos/finalizar.");
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

    /** Crea un intento antes de mostrar las preguntas, para medir el tiempo desde el servidor. */
    private void iniciarIntento(JsonObject json, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();

        Long idUsuario = numero(json, "idUsuario");
        Long idTest = numero(json, "idTestNumerico");
        if (idUsuario == null || idTest == null) {
            error(resp, 400, "Faltan datos para iniciar el intento.");
            return;
        }

        Usuario usuario = em.find(Usuario.class, idUsuario);
        TestNumerico test = em.find(TestNumerico.class, idTest);
        if (usuario == null || test == null) {
            error(resp, 404, "Usuario o test inexistente.");
            return;
        }
        if (!usuario.isActivo() || !test.isActivo()) {
            error(resp, 403, "El usuario o el test no esta activo.");
            return;
        }

        List<Pregunta> preguntas = obtenerPreguntasActivas(em, idTest);
        if (preguntas.isEmpty()) {
            error(resp, 409, "Este test no tiene preguntas activas.");
            return;
        }

        IntentoTest intento = new IntentoTest();
        intento.setUsuario(usuario);
        intento.setTestNumerico(test);
        intento.setCantidadPreguntas(preguntas.size());
        intento.iniciar();
        em.persist(intento);
        em.flush();

        JsonObject salida = new JsonObject();
        salida.addProperty("idIntentoTest", intento.getIdIntentoTest());
        salida.addProperty("fechaInicio", intento.getFechaInicio().toString());
        salida.addProperty("tiempoLimiteSegundos", TestNumerico.TIEMPO_LIMITE_MINUTOS * 60);
        salida.add("preguntas", preguntasComoJson(preguntas));
        escribir(resp, 200, salida);
    }

    /** Finaliza un intento ya iniciado y califica sin exponer resultados al candidato. */
    private void finalizarIntento(JsonObject json, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();

        Long idIntento = numero(json, "idIntentoTest");
        Long idUsuario = numero(json, "idUsuario");
        if (idIntento == null || idUsuario == null) {
            error(resp, 400, "Faltan datos para finalizar el intento.");
            return;
        }

        IntentoTest intento = em.find(IntentoTest.class, idIntento);
        if (intento == null) {
            error(resp, 404, "Intento inexistente.");
            return;
        }
        if (intento.getUsuario() == null || !idUsuario.equals(intento.getUsuario().getIdUsuario())) {
            error(resp, 403, "El intento no pertenece al usuario indicado.");
            return;
        }
        if (!intento.estaEnProgreso()) {
            error(resp, 409, "El intento ya fue finalizado.");
            return;
        }

        List<JsonObject> entradas = obtenerEntradasRespuestas(json);
        registrarRespuestas(em, intento, entradas);

        boolean excedioTiempo = intento.excedeTiempoLimite(TestNumerico.TIEMPO_LIMITE_MINUTOS);
        if (excedioTiempo) {
            intento.cerrarPorTiempo();
        } else {
            intento.finalizar();
        }
        intento.calcularResultado();

        JsonObject salida = new JsonObject();
        salida.addProperty("idIntentoTest", intento.getIdIntentoTest());
        salida.addProperty("estado", intento.getEstado().name());
        salida.addProperty("cerradoPorTiempo", excedioTiempo);
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
            o.addProperty("estado", i.getEstado() != null ? i.getEstado().name() : "");
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

    private String normalizarRespuesta(String respuesta) {
        if (respuesta == null) {
            return null;
        }
        String valor = respuesta.trim().toUpperCase();
        return valor.equals("A") || valor.equals("B") ? valor : null;
    }

    private List<Pregunta> obtenerPreguntasActivas(EntityManager em, Long idTest) {
        return em.createQuery(
                "from Pregunta p where p.testNumerico.idTestNumerico = :id and p.activa = true "
                + "order by p.numeroPregunta", Pregunta.class)
                .setParameter("id", idTest)
                .getResultList();
    }

    private JsonArray preguntasComoJson(List<Pregunta> preguntas) {
        JsonArray arreglo = new JsonArray();
        for (Pregunta p : preguntas) {
            JsonObject o = new JsonObject();
            o.addProperty("idPregunta", p.getIdPregunta());
            o.addProperty("numeroPregunta", p.getNumeroPregunta());
            o.addProperty("operacion", p.getOperacion());
            o.addProperty("resultadoMostrado", p.getResultadoMostrado());
            arreglo.add(o);
        }
        return arreglo;
    }

    private List<JsonObject> obtenerEntradasRespuestas(JsonObject json) {
        List<JsonObject> entradas = new ArrayList<>();
        if (json.has("respuestas") && json.get("respuestas").isJsonArray()) {
            for (var elem : json.getAsJsonArray("respuestas")) {
                entradas.add(elem.getAsJsonObject());
            }
        }
        return entradas;
    }

    private void registrarRespuestas(EntityManager em, IntentoTest intento, List<JsonObject> entradas) {
        Long idTest = intento.getTestNumerico() != null ? intento.getTestNumerico().getIdTestNumerico() : null;
        Set<Long> preguntasProcesadas = new HashSet<>();
        for (JsonObject entrada : entradas) {
            Long idPregunta = numero(entrada, "idPregunta");
            if (idPregunta == null || !preguntasProcesadas.add(idPregunta)) {
                continue;
            }

            Pregunta pregunta = em.find(Pregunta.class, idPregunta);
            if (pregunta == null
                    || !pregunta.isActiva()
                    || pregunta.getTestNumerico() == null
                    || !pregunta.getTestNumerico().getIdTestNumerico().equals(idTest)) {
                continue;
            }

            Respuesta r = new Respuesta();
            r.setPregunta(pregunta);
            r.setRespuestaMarcada(normalizarRespuesta(texto(entrada, "respuestaMarcada")));
            intento.registrarRespuesta(r);
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
