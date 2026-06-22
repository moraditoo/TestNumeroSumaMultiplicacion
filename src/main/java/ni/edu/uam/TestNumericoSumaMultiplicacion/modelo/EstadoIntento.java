package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

/**
 * Estados posibles de un intento de test.
 *  - EN_PROGRESO: el usuario esta respondiendo.
 *  - FINALIZADO : el usuario respondio todas las preguntas dentro del tiempo.
 *  - CANCELADO  : el tiempo se agoto antes de terminar.
 *  - CALIFICADO : ya se calcularon aciertos, errores y preguntas sin responder.
 */
public enum EstadoIntento {
    EN_PROGRESO,
    FINALIZADO,
    CANCELADO,
    CALIFICADO
}
