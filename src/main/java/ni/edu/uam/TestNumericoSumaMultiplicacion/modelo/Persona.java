package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.constraints.Size;

import org.openxava.annotations.ReadOnly;
import org.openxava.annotations.Required;
import org.openxava.annotations.Stereotype;
import org.openxava.annotations.Hidden;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ni.edu.uam.TestNumericoSumaMultiplicacion.util.PasswordUtil;

/**
 * Clase base para las personas del sistema (Registrador y Usuario).
 *
 * Centraliza los datos comunes y, sobre todo, las reglas que deben cumplirse
 * SIEMPRE que se guarda una persona:
 *   - La contrasena se almacena cifrada (BCrypt), nunca en texto plano.
 *   - La fecha de registro se asigna sola al crear el registro.
 *   - El registro nace activo por defecto.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Persona {

    private boolean activo;

    @Required
    @Column(length = 50)
    @Size(max = 50, message = "Los nombres no pueden superar los 50 caracteres")
    private String nombres;

    @Required
    @Column(length = 50)
    @Size(max = 50, message = "Los apellidos no pueden superar los 50 caracteres")
    private String apellidos;

    @Required
    @Column(length = 30, unique = true)
    @Size(min = 3, max = 30, message = "El usuario debe tener entre 3 y 30 caracteres")
    private String nombreUsuario;

    @Required
    @Stereotype("EMAIL")
    @Column(length = 100, unique = true)
    private String correo;

    @Required
    @Stereotype("PASSWORD")
    @Column(length = 100)
    @Size(min = 4, message = "La contrasena debe tener al menos 4 caracteres")
    private String contrasena;

    @Hidden
    private LocalDateTime fechaRegistro;

    /**
     * Se ejecuta justo antes de INSERTAR la persona en la base de datos.
     * Asigna la fecha de registro, marca el registro como activo y cifra la
     * contrasena si todavia esta en texto plano.
     */
    @PrePersist
    private void alRegistrar() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        activo = true;
        cifrarContrasenaSiHaceFalta();
    }

    /**
     * Se ejecuta antes de ACTUALIZAR. Solo cifra la contrasena cuando el
     * administrador la ha cambiado por una nueva en texto plano; si el valor
     * ya es un hash, no se vuelve a cifrar (evita romper el login).
     */
    @PreUpdate
    private void alActualizar() {
        cifrarContrasenaSiHaceFalta();
    }

    private void cifrarContrasenaSiHaceFalta() {
        if (contrasena != null && !contrasena.isBlank() && !PasswordUtil.estaCifrada(contrasena)) {
            contrasena = PasswordUtil.cifrar(contrasena);
        }
    }
}
