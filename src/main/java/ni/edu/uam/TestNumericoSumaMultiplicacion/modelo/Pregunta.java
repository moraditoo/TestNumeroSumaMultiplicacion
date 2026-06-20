package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

import java.time.LocalDateTime;

import javax.persistence.*;

import org.openxava.annotations.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPregunta;

    @Required
    private int numeroPregunta;

    private boolean activa;

    private LocalDateTime fechaCreacion;

    @Required
    @Column(length = 100)
    private String operacion;

    @Required
    private int resultadoMostrado;

    @Required
    @Column(length = 1)
    private String respuestaCorrecta;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties = "nombreUsuario")
    private Registrador registrador;

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    public boolean validarDatos() {
        return operacion != null
                && !operacion.isBlank()
                && (respuestaCorrecta.equals("A")
                || respuestaCorrecta.equals("B"));
    }

    public boolean verificarRespuesta(String respuesta) {
        return respuestaCorrecta.equalsIgnoreCase(respuesta);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList
    private TestNumerico testNumerico;

}
