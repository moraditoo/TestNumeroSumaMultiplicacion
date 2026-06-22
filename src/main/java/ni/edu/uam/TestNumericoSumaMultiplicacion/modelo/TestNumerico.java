package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Pattern.Flag;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.openxava.annotations.*;

/**
 * Representa un test numerico (Sumas o Multiplicaciones).
 * Mantiene la coleccion de preguntas y el tiempo limite de aplicacion.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestNumerico {

    public static final int TIEMPO_LIMITE_MINUTOS = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTestNumerico;

    @Required
    @Column(length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    private boolean activo;

    @Required
    @Column(length = 20)
    @Pattern(regexp = "SUMAS|MULTIPLICACIONES", flags = Flag.CASE_INSENSITIVE,
             message = "El tipo de test solo puede ser SUMAS o MULTIPLICACIONES")
    private String tipoTest;

    @ListProperties("numeroPregunta, operacion, resultadoMostrado, respuestaCorrecta, activa")
    @OneToMany(mappedBy = "testNumerico")
    private List<Pregunta> preguntas = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void normalizar() {
        if (tipoTest != null) {
            tipoTest = tipoTest.trim().toUpperCase();
        }
    }

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
