package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

import java.time.LocalDateTime;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.openxava.annotations.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pregunta de un test numerico. Presenta una operacion y un "resultado mostrado";
 * la respuesta correcta indica si ese resultado es Verdadero (A) o Falso (B).
 */
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
    @Min(value = 1, message = "El numero de pregunta debe ser mayor que cero")
    private Integer numeroPregunta;

    private boolean activa;

    @ReadOnly
    private LocalDateTime fechaCreacion;

    @Required
    @Column(length = 100)
    private String operacion;

    @Required
    private Integer resultadoMostrado;

    @Required
    @Column(length = 1)
    @Pattern(regexp = "[AaBb]", message = "La respuesta correcta solo puede ser A (Verdadero) o B (Falso)")
    private String respuestaCorrecta;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties = "nombreUsuario")
    private Registrador registrador;

    @Required
    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties = "nombre")
    private TestNumerico testNumerico;

    /**
     * Al crear la pregunta queda activa por defecto y se sella la fecha de creacion.
     */
    @PrePersist
    private void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        // Una pregunta nueva nace activa salvo que se indique lo contrario.
        if (respuestaCorrecta != null) {
            respuestaCorrecta = respuestaCorrecta.trim().toUpperCase();
        }
    }

    @PreUpdate
    private void alModificar() {
        if (respuestaCorrecta != null) {
            respuestaCorrecta = respuestaCorrecta.trim().toUpperCase();
        }
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    public boolean validarDatos() {
        return operacion != null
                && !operacion.isBlank()
                && respuestaCorrecta != null
                && (respuestaCorrecta.equals("A") || respuestaCorrecta.equals("B"));
    }

    public boolean verificarRespuesta(String respuesta) {
        return respuestaCorrecta != null && respuestaCorrecta.equalsIgnoreCase(respuesta);
    }
}
