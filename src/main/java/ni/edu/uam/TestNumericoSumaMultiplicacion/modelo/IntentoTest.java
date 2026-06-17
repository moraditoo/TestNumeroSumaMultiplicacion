package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;


import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.openxava.annotations.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntentoTest {

    private static final String MENSAJE_FINAL = "Muchas gracias por completar el test.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idIntentoTest;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList
    private TestNumerico testNumerico;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    private EstadoIntento estado;

    private int cantidadPreguntas;

    private int aciertos;

    private int errores;

    private int sinResponder;

    public void iniciar() {
        estado = EstadoIntento.EN_PROGRESO;
        fechaInicio = LocalDateTime.now();
    }

    public void registrarRespuesta(Respuesta respuesta) {
        // Se implementará después
    }

    public void finalizar() {
        estado = EstadoIntento.FINALIZADO;
        fechaFin = LocalDateTime.now();
    }

    public void cerrarPorTiempo() {
        estado = EstadoIntento.FINALIZADO;
        fechaFin = LocalDateTime.now();
    }

    public void calcularResultado(List<Respuesta> respuestas) {

        aciertos = 0;
        errores = 0;
        sinResponder = 0;

        for (Respuesta respuesta : respuestas) {

            if (respuesta.estaEnBlanco()) {
                sinResponder++;
            }
            else if (respuesta.esCorrecta()) {
                aciertos++;
            }
            else {
                errores++;
            }
        }
    }

    public int calcularTiempoUsado() {

        if (fechaInicio == null || fechaFin == null) {
            return 0;
        }

        return (int) java.time.Duration
                .between(fechaInicio, fechaFin)
                .toMinutes();
    }

    public String generarMensajeFinal() {
        return MENSAJE_FINAL;
    }

    public boolean estaEnProgreso() {
        return estado == EstadoIntento.EN_PROGRESO;
    }

    @OneToMany(mappedBy = "intentoTest")
    private List<Respuesta> respuestas;
}