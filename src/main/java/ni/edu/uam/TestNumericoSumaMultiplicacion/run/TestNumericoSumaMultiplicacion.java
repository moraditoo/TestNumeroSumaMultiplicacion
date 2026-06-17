package ni.edu.uam.TestNumericoSumaMultiplicacion.run;

import org.openxava.util.*;

/**
 * Ejecuta esta clase para arrancar la aplicación.
 */

public class TestNumericoSumaMultiplicacion {

	public static void main(String[] args) throws Exception {
		DBServer.start("TestNumericoSumaMultiplicacion-db"); // Para usar tu propia base de datos comenta esta línea y configura src/main/webapp/META-INF/context.xml
		AppServer.run("TestNumericoSumaMultiplicacion"); // Usa AppServer.run("") para funcionar en el contexto raíz
	}

}
