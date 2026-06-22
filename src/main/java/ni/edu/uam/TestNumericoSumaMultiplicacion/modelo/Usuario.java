package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.openxava.annotations.Required;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Usuario que realiza los test. Hereda de Persona los datos de acceso
 * (usuario, correo, contrasena cifrada) y las reglas de registro.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario extends Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Required
    @Min(value = 5, message = "La edad minima permitida es 5 anios")
    @Max(value = 120, message = "La edad maxima permitida es 120 anios")
    private Integer edad;

    @OneToMany(mappedBy = "usuario")
    private List<IntentoTest> intentos;

}
