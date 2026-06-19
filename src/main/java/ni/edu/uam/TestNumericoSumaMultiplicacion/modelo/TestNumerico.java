package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

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

    @ListProperties("numeroPregunta, operacion")
    @OneToMany(mappedBy = "testNumerico")
    private List<Pregunta> preguntas = new ArrayList<>();

    public List<Pregunta> obtenerPreguntasActivas() {
        return preguntas.stream()
                .filter(Pregunta::isActiva)
                .toList();
    }

    public boolean validarDisponibilidad() {
        return activo;
    }

    public IntentoTest iniciarTest(Usuario usuario) {
        return new IntentoTest();
    }

    public void agregarPregunta(Pregunta pregunta) {
        preguntas.add(pregunta);
    }

    public void desactivarTest() {
        this.activo = false;
    }

}