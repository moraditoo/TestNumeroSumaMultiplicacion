package ni.edu.uam.TestNumericoSumaMultiplicacion.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad para cifrar y verificar contrasenas usando el algoritmo BCrypt.
 *
 * BCrypt incorpora una "sal" (salt) aleatoria dentro del propio hash, por lo que
 * dos usuarios con la misma contrasena tendran hashes distintos. El hash resultante
 * empieza siempre por "$2a$", "$2b$" o "$2y$", lo que nos permite detectar si un
 * valor ya esta cifrado y evitar volver a cifrarlo en las actualizaciones.
 */
public final class PasswordUtil {

    /** Coste de trabajo de BCrypt (10 = ~100ms, valor recomendado por defecto). */
    private static final int COSTE = 10;

    private PasswordUtil() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Cifra una contrasena en texto plano y devuelve el hash BCrypt.
     */
    public static String cifrar(String textoPlano) {
        if (textoPlano == null) {
            return null;
        }
        return BCrypt.hashpw(textoPlano, BCrypt.gensalt(COSTE));
    }

    /**
     * Comprueba si una contrasena en texto plano coincide con el hash almacenado.
     */
    public static boolean coincide(String textoPlano, String hashAlmacenado) {
        if (textoPlano == null || hashAlmacenado == null || hashAlmacenado.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(textoPlano, hashAlmacenado);
        } catch (IllegalArgumentException ex) {
            // El hash almacenado no tiene formato BCrypt valido.
            return false;
        }
    }

    /**
     * Indica si el valor recibido ya es un hash BCrypt (para no cifrarlo dos veces).
     */
    public static boolean estaCifrada(String valor) {
        return valor != null && valor.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}
