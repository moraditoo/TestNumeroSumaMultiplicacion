package ni.edu.uam.TestNumericoSumaMultiplicacion.vista;

import lombok.Getter;
import lombok.Setter;
import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.IntentoTest;
import ni.edu.uam.TestNumericoSumaMultiplicacion.modelo.Pregunta;

@Getter
@Setter
public class ExamenView {

    private IntentoTest intento;

    private Pregunta preguntaActual;

    private int numeroPreguntaActual;

    private int totalPreguntas;

    private long tiempoRestante;

    private String respuestaSeleccionada;

}
