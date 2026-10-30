package com.sgtg.backend.domain.models.acceso;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Requisitos de una contraseña nueva:
 * <ul>
 * <li>entre {@value #LONGITUD_MINIMA} y {@value #LONGITUD_MAXIMA} caracteres,
 * sin espacios al inicio ni al final;</li>
 * <li>al menos una minúscula, una mayúscula, un número y un símbolo;</li>
 * <li>que no contenga el usuario del correo, el número de documento ni palabras
 * fáciles de adivinar ({@link #PALABRAS_COMUNES}).</li>
 * </ul>
 * Además, al cambiarla el usuario no puede repetir ninguna de sus últimas
 * {@value #CONTRASENAS_RECORDADAS} contraseñas; eso lo revisa el servicio de
 * autenticación, que tiene los hashes.
 */
public final class PoliticaPassword {

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA = 64;
    /** Contraseñas recientes (incluida la actual) que el usuario no puede repetir */
    public static final int CONTRASENAS_RECORDADAS = 5;
    /** Límite de BCrypt: lo que pase de 72 bytes no se tendría en cuenta */
    private static final int BYTES_MAXIMOS = 72;
    /** Partes del correo o del documento más cortas que esto no se buscan (habría muchos falsos positivos) */
    private static final int LONGITUD_MINIMA_DATO = 4;
    /** Palabras y secuencias que no puede contener, sin tildes y en minúsculas */
    static final List<String> PALABRAS_COMUNES = List.of("password", "contrasena", "unicauca", "sgtg", "qwerty",
            "123456", "abcdef");

    /** Resultado de revisar una contraseña contra la política */
    public enum Resultado {
        VALIDA,
        /** No cumple la longitud o la combinación de caracteres */
        INVALIDA,
        /** Cumple el formato pero contiene datos del usuario o palabras comunes */
        PREDECIBLE
    }

    private PoliticaPassword() {
    }

    /**
     * @param password        contraseña nueva en texto plano
     * @param email           correo del usuario
     * @param numeroDocumento número de documento del usuario
     */
    public static Resultado revisar(String password, String email, String numeroDocumento) {
        if (!tieneFormatoValido(password))
            return Resultado.INVALIDA;
        String normalizada = normalizar(password);
        if (PALABRAS_COMUNES.stream().anyMatch(normalizada::contains))
            return Resultado.PREDECIBLE;
        if (contieneDato(normalizada, usuarioDelCorreo(email)) || contieneDato(normalizada, numeroDocumento))
            return Resultado.PREDECIBLE;
        return Resultado.VALIDA;
    }

    private static boolean tieneFormatoValido(String password) {
        if (password == null || password.length() < LONGITUD_MINIMA || password.length() > LONGITUD_MAXIMA)
            return false;
        if (!password.equals(password.strip()) || password.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS)
            return false;
        return password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
    }

    private static String usuarioDelCorreo(String email) {
        if (email == null)
            return null;
        int arroba = email.indexOf('@');
        return arroba < 0 ? email : email.substring(0, arroba);
    }

    private static boolean contieneDato(String normalizada, String dato) {
        if (dato == null)
            return false;
        String datoNormalizado = normalizar(dato);
        return datoNormalizado.length() >= LONGITUD_MINIMA_DATO && normalizada.contains(datoNormalizado);
    }

    /** En minúsculas y sin tildes, para que "Contraseña" y "CONTRASENA" cuenten igual */
    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
