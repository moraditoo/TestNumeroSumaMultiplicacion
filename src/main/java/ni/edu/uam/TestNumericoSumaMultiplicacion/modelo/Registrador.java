package ni.edu.uam.TestNumericoSumaMultiplicacion.modelo;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Registrador extends Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRegistrador;

    @OneToMany(mappedBy = "registrador")
    private List<Pregunta> preguntasRegistradas;

}