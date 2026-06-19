package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

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
public class Respuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRespuesta;

    @Required
    @Column(length = 1)
    private String respuestaMarcada;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties = "numeroPregunta")
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties = "idIntentoTest")
    private IntentoTest intentoTest;

    public boolean estaEnBlanco() {
        return respuestaMarcada == null || respuestaMarcada.isBlank();
    }

    public boolean esCorrecta() {
        return pregunta != null &&
                respuestaMarcada != null &&
                respuestaMarcada.equalsIgnoreCase(
                        pregunta.getRespuestaCorrecta()
                );
    }
}
