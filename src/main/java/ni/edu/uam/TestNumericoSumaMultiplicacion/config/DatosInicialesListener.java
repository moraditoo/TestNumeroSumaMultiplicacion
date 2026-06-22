package ni.edu.uam.TestNumericoSumaMultiplicacion.config;

import javax.persistence.EntityManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.openxava.jpa.XPersistence;

import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.Pregunta;
import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.Registrador;
import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.TestNumerico;

/**
 * Carga inicial de datos. Al arrancar la aplicacion, si todavia no existen test
 * registrados, crea automaticamente:
 *   - Un registrador por defecto (con contrasena cifrada).
 *   - Los dos test: Numerico Sumas y Numerico Multiplicaciones.
 *   - Todas las preguntas de los cuadernillos, con su respuesta correcta verificada.
 *
 * Es idempotente: si ya hay datos, no hace nada, de modo que el administrador
 * puede seguir agregando o editando preguntas desde OpenXava sin que se dupliquen.
 */
public class DatosInicialesListener implements ServletContextListener {

    // {numeroPregunta, operacion, resultadoMostrado, respuestaCorrecta}
    private static final String[][] SUMAS = {
            {"3", "23 + 46 + 57 + 38", "164", "A"},
            {"4", "17 + 39 + 42 + 25", "113", "B"},
            {"5", "56 + 49 + 77 + 94", "276", "A"},
            {"6", "48 + 55 + 29 + 63", "195", "A"},
            {"7", "64 + 37 + 99 + 58", "268", "B"},
            {"8", "84 + 73 + 68 + 35", "250", "B"},
            {"9", "95 + 83 + 47 + 36", "271", "B"},
            {"10", "54 + 87 + 96 + 19", "256", "A"},
            {"11", "18 + 27 + 76 + 89", "210", "A"},
            {"12", "98 + 89 + 32 + 74", "283", "B"},
            {"13", "86 + 79 + 92 + 67", "324", "A"},
            {"14", "68 + 75 + 44 + 16", "191", "B"},
            {"15", "99 + 77 + 92 + 57", "325", "A"},
            {"16", "73 + 31 + 97 + 24", "225", "A"},
            {"17", "26 + 86 + 68 + 79", "249", "B"},
            {"18", "523 + 742 + 687 + 864", "2826", "B"},
            {"19", "241 + 923 + 576 + 438", "2178", "A"},
            {"20", "326 + 489 + 718 + 993", "2526", "A"},
            {"21", "432 + 627 + 368 + 878", "2203", "B"},
            {"22", "156 + 283 + 939 + 855", "2243", "B"},
            {"23", "724 + 467 + 659 + 872", "2722", "A"},
            {"24", "234 + 999 + 876 + 363", "2374", "B"},
            {"25", "361 + 648 + 876 + 453", "2338", "A"},
            {"26", "273 + 942 + 357 + 834", "2406", "A"},
            {"27", "562 + 447 + 688 + 955", "2550", "B"},
    };

    private static final String[][] MULTIPLICACIONES = {
            {"30", "82 x 3", "246", "A"},
            {"31", "54 x 4", "206", "B"},
            {"32", "36 x 8", "298", "B"},
            {"33", "79 x 6", "474", "A"},
            {"34", "67 x 9", "603", "A"},
            {"35", "763 x 7", "5241", "B"},
            {"36", "546 x 4", "2284", "B"},
            {"37", "435 x 8", "3480", "A"},
            {"38", "984 x 6", "5804", "B"},
            {"39", "798 x 7", "5586", "A"},
            {"40", "3486 x 3", "11458", "B"},
            {"41", "4128 x 7", "29896", "B"},
            {"42", "5749 x 5", "28745", "A"},
            {"43", "2976 x 4", "12904", "B"},
            {"44", "9489 x 6", "56934", "A"},
            {"45", "43638 x 8", "349104", "A"},
            {"46", "53264 x 7", "373848", "B"},
            {"47", "36578 x 9", "329202", "A"},
            {"48", "26773 x 8", "214184", "A"},
            {"49", "66399 x 6", "397394", "B"},
            {"50", "624085 x 3", "1872255", "A"},
            {"51", "529724 x 5", "2649620", "B"},
            {"52", "478907 x 7", "3342349", "B"},
            {"53", "683218 x 8", "5465744", "A"},
            {"54", "732363 x 6", "4404178", "B"},
    };

    /** Bandera para no repetir el intento de siembra en cada peticion. */
    private static volatile boolean yaIntentado = false;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sembrarSiVacio();
    }

    /**
     * Siembra los datos iniciales si la base esta vacia. Es idempotente y se puede
     * invocar tanto al arrancar como en la primera peticion del API (lo que ocurra
     * primero), garantizando que las entidades de OpenXava ya esten registradas.
     */
    public static synchronized void sembrarSiVacio() {
        if (yaIntentado) {
            return;
        }
        try {
            EntityManager em = XPersistence.getManager();

            Long existentes = em.createQuery(
                    "select count(t) from TestNumerico t", Long.class).getSingleResult();
            if (existentes != null && existentes > 0) {
                XPersistence.commit();
                yaIntentado = true;
                return; // Ya hay datos: no se vuelve a sembrar.
            }

            Registrador registrador = new Registrador();
            registrador.setNombres("Registrador");
            registrador.setApellidos("del Sistema");
            registrador.setNombreUsuario("registrador");
            registrador.setCorreo("registrador@uam.edu.ni");
            registrador.setContrasena("registrador123"); // se cifra sola en @PrePersist
            em.persist(registrador);

            TestNumerico sumas = crearTest(em,
                    "Numerico Sumas",
                    "Verifique si el resultado de cada suma es Verdadero (A) o Falso (B).",
                    "SUMAS");

            TestNumerico multi = crearTest(em,
                    "Numerico Multiplicaciones",
                    "Verifique si el resultado de cada multiplicacion es Verdadero (A) o Falso (B).",
                    "MULTIPLICACIONES");

            for (String[] fila : SUMAS) {
                crearPregunta(em, sumas, registrador, fila);
            }
            for (String[] fila : MULTIPLICACIONES) {
                crearPregunta(em, multi, registrador, fila);
            }

            XPersistence.commit();
            yaIntentado = true;
            System.out.println(">> Datos iniciales cargados: 2 test y "
                    + (SUMAS.length + MULTIPLICACIONES.length) + " preguntas.");
        } catch (Exception ex) {
            XPersistence.rollback();
            // No se marca yaIntentado: se reintentara en la primera peticion del API,
            // cuando OpenXava ya tenga registradas todas las entidades.
            System.err.println(">> Aviso: la siembra inicial se hara en la primera peticion. Detalle: " + ex.getMessage());
        }
    }

    private static TestNumerico crearTest(EntityManager em, String nombre, String descripcion, String tipo) {
        TestNumerico t = new TestNumerico();
        t.setNombre(nombre);
        t.setDescripcion(descripcion);
        t.setTipoTest(tipo);
        t.setActivo(true);
        em.persist(t);
        return t;
    }

    private static void crearPregunta(EntityManager em, TestNumerico test, Registrador registrador, String[] fila) {
        Pregunta p = new Pregunta();
        p.setNumeroPregunta(Integer.parseInt(fila[0]));
        p.setOperacion(fila[1]);
        p.setResultadoMostrado(Integer.parseInt(fila[2]));
        p.setRespuestaCorrecta(fila[3]);
        p.setActiva(true);
        p.setTestNumerico(test);
        p.setRegistrador(registrador);
        em.persist(p);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // No requiere limpieza.
    }
}
