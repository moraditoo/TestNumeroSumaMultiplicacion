package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import org.openxava.annotations.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de un intento de test realizado por un usuario.
 * Guarda las respuestas, calcula el resultado (aciertos, errores y sin responder)
 * y controla el estado del intento.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntentoTest {

    public static final String MENSAJE_FINAL = "Muchas gracias por realizar el test.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idIntentoTest;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties = "nombreUsuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties = "nombre")
    private TestNumerico testNumerico;

    @ReadOnly
    private LocalDateTime fechaInicio;

    @ReadOnly
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private EstadoIntento estado;

    private int cantidadPreguntas;

    private int aciertos;

    private int errores;

    private int sinResponder;

    @OneToMany(mappedBy = "intentoTest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Respuesta> respuestas = new ArrayList<>();

    public void iniciar() {
        estado = EstadoIntento.EN_PROGRESO;
        fechaInicio = LocalDateTime.now();
    }

    public void registrarRespuesta(Respuesta respuesta) {
        if (respuestas == null) {
            respuestas = new ArrayList<>();
        }
        respuesta.setIntentoTest(this);
        respuestas.add(respuesta);
    }

    public void finalizar() {
        estado = EstadoIntento.FINALIZADO;
        fechaFin = LocalDateTime.now();
    }

    public void cerrarPorTiempo() {
        estado = EstadoIntento.CANCELADO;
        fechaFin = LocalDateTime.now();
    }

    /**
     * Recorre las respuestas y cuenta aciertos, errores y preguntas sin responder.
     * Al terminar deja el intento en estado CALIFICADO.
     */
    public void calcularResultado() {
        aciertos = 0;
        errores = 0;
        sinResponder = 0;

        if (respuestas != null) {
            for (Respuesta respuesta : respuestas) {
                if (respuesta.estaEnBlanco()) {
                    sinResponder++;
                } else if (respuesta.esCorrecta()) {
                    aciertos++;
                } else {
                    errores++;
                }
            }
        }

        // Las preguntas que el usuario nunca llego a ver tambien cuentan como sin responder.
        int respondidasOEnBlanco = aciertos + errores + sinResponder;
        if (cantidadPreguntas > respondidasOEnBlanco) {
            sinResponder += (cantidadPreguntas - respondidasOEnBlanco);
        }

        estado = EstadoIntento.CALIFICADO;
    }

    public int calcularTiempoUsado() {
        if (fechaInicio == null || fechaFin == null) {
            return 0;
        }
        return (int) java.time.Duration.between(fechaInicio, fechaFin).toSeconds();
    }

    public String generarMensajeFinal() {
        return MENSAJE_FINAL;
    }

    public boolean estaEnProgreso() {
        return estado == EstadoIntento.EN_PROGRESO;
    }
}
