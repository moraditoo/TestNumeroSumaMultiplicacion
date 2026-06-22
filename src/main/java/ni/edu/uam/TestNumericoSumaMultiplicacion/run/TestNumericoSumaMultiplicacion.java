package ni.edu.uam.TestNumericoSumaMultiplicacion.run;

import org.openxava.util.*;

/**
 * Ejecuta esta clase para arrancar la aplicacion.
 */

public class TestNumericoSumaMultiplicacion {

	public static void main(String[] args) throws Exception {
		// DBServer.start("TestNumericoSumaMultiplicacion-db"); // Desactivado: se usa PostgreSQL (Estudiantedb) configurado en src/main/webapp/META-INF/context.xml
		AppServer.run("TestNumericoSumaMultiplicacion"); // Usa AppServer.run("") para funcionar en el contexto raiz
	}

}
