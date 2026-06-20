package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class TestNumerico {

    private static final int TIEMPO_LIMITE_MINUTOS = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTestNumerico;

    @Required
    @Column(length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    private boolean activo;

    private String tipoTest;

    @ListProperties("numeroPregunta, operacion")
    @OneToMany(mappedBy = "testNumerico")
    private List<Pregunta> preguntas = new ArrayList<>();

    public List<Pregunta> obtenerPreguntasActivas() {
        return preguntas.stream()
                .filter(Pregunta::isActiva)
                .toList();
    }

    public boolean validarDisponibilidad() {
        return activo && !obtenerPreguntasActivas().isEmpty();
    }

    public IntentoTest iniciarTest(Usuario usuario) {

        IntentoTest intento = new IntentoTest();

        intento.setUsuario(usuario);
        intento.setTestNumerico(this);
        intento.setCantidadPreguntas(obtenerPreguntasActivas().size());

        intento.iniciar();

        return intento;
    }

    public void agregarPregunta(Pregunta pregunta) {

        pregunta.setTestNumerico(this);

        preguntas.add(pregunta);
    }

    public void desactivarTest() {
        this.activo = false;
    }

}