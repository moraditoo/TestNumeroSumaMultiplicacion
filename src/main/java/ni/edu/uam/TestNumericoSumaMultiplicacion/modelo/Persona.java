package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Persona {

    private boolean activo;

    @Column(length = 50)
    private String nombres;

    @Column(length = 50)
    private String apellidos;

    @Column(length = 30, unique = true)
    private String nombreUsuario;

    @Column(length = 100, unique = true)
    private String correo;

    @Column(length = 100)
    private String contrasena;

    private LocalDateTime fechaRegistro;

}
