package com.sgtg.backend.domain.models.auditoria;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Sello de un registro de auditoría dentro de la cadena: HMAC-SHA256, con una
 * clave que no está en la base de datos, del número del registro en la
 * cadena, el sello del registro anterior y todos los datos del registro.
 * <p>
 * Así, quien tenga acceso a la base pero no a la clave no puede:
 * <ul>
 * <li>modificar un registro: su sello deja de coincidir;</li>
 * <li>borrar un registro: falta su número y el sello del siguiente no coincide;</li>
 * <li>agregar uno falso: no puede calcular su sello.</li>
 * </ul>
 */
public final class SelloAuditoria {

    private static final String ALGORITMO = "HmacSHA256";

    private SelloAuditoria() {
    }

    /**
     * Clave de los sellos derivada de otra (HMAC de un texto fijo), para no
     * usar la misma clave en dos cosas distintas.
     */
    public static byte[] derivarClave(byte[] claveBase) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(claveBase, ALGORITMO));
            return mac.doFinal("sgtg-auditoria".getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo derivar la clave de la auditoría", e);
        }
    }

    /**
     * @param clave         clave del HMAC
     * @param secuencia     número del registro en la cadena (1, 2, 3...)
     * @param selloAnterior sello del registro anterior; null en el primero
     * @return sello en hexadecimal (64 caracteres)
     */
    public static String sellar(byte[] clave, long secuencia, String selloAnterior, RegistroAuditoria r) {
        StringBuilder datos = new StringBuilder();
        // Cada valor con su longitud delante: ningún contenido se puede confundir con un separador
        for (Object valor : new Object[] { secuencia, selloAnterior, r.fecha(), r.usuarioId(), r.usuario(),
                r.roles(), r.sesion(), r.solicitud(), r.ip(), r.agenteUsuario(), r.metodo(), r.ruta(), r.operacion(),
                r.trabajoId(), r.parametros(), r.estadoHttp(), r.resultado(), r.detalle() }) {
            String texto = valor == null ? null : valor.toString();
            datos.append(texto == null ? -1 : texto.length()).append(':').append(texto == null ? "" : texto)
                    .append(';');
        }
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(clave, ALGORITMO));
            return HexFormat.of().formatHex(mac.doFinal(datos.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo sellar el registro de auditoría", e);
        }
    }
}
