package com.sgtg.backend.domain.use_cases;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.sgtg.backend.application.input.seguridad.ManageSeguridadCUIntPort;
import com.sgtg.backend.application.output.ManageAlertaGatewayIntPort;
import com.sgtg.backend.application.output.ManageArchivoGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuditoriaGatewayIntPort;
import com.sgtg.backend.domain.models.acceso.IntentosAcceso;
import com.sgtg.backend.domain.models.auditoria.AlertaSeguridad;
import com.sgtg.backend.domain.models.auditoria.TipoAlerta;
import com.sgtg.backend.domain.models.auditoria.VerificacionCadena;

/**
 * Reglas de las alertas de seguridad. Revisa la auditoría de los últimos
 * {@link #VENTANA} minutos:
 * <ul>
 * <li>{@value #LOGINS_FALLIDOS_POR_IP} o más inicios de sesión fallidos desde
 * una IP;</li>
 * <li>un correo con los fallos que lo bloquean
 * ({@value IntentosAcceso#MAXIMO_FALLOS});</li>
 * <li>{@value #ACCESOS_DENEGADOS} o más operaciones sin permiso de un
 * usuario;</li>
 * <li>cualquier token válido rechazado (sesión cerrada, usuario inactivo o
 * token anterior a un cambio de contraseña).</li>
 * </ul>
 * La verificación de integridad recorre la cadena de la auditoría y las huellas
 * de los archivos. Usa el reloj real, no el ajustable de desarrollo.
 */
public class ManageSeguridadCUImplAdapter implements ManageSeguridadCUIntPort {

    /** Tiempo hacia atrás que se revisa en cada detección */
    public static final Duration VENTANA = Duration.ofMinutes(15);
    public static final int LOGINS_FALLIDOS_POR_IP = 10;
    public static final int ACCESOS_DENEGADOS = 5;

    private final ManageAuditoriaGatewayIntPort auditoria;
    private final ManageArchivoGatewayIntPort archivos;
    private final ManageAlertaGatewayIntPort alertas;
    private final Clock reloj;

    public ManageSeguridadCUImplAdapter(ManageAuditoriaGatewayIntPort auditoria, ManageArchivoGatewayIntPort archivos,
            ManageAlertaGatewayIntPort alertas, Clock reloj) {
        this.auditoria = auditoria;
        this.archivos = archivos;
        this.alertas = alertas;
        this.reloj = reloj;
    }

    @Override
    public int detectarAlertas() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime desde = ahora.minus(VENTANA);
        int nuevas = 0;
        for (String ip : auditoria.ipsConLoginsFallidos(desde, LOGINS_FALLIDOS_POR_IP))
            nuevas += avisar(TipoAlerta.FUERZA_BRUTA_IP, ip, "Desde la IP " + ip + " hubo " + LOGINS_FALLIDOS_POR_IP
                    + " o más inicios de sesión fallidos en los últimos " + VENTANA.toMinutes() + " minutos.", ahora);
        for (String correo : auditoria.correosConLoginsFallidos(desde, IntentosAcceso.MAXIMO_FALLOS))
            nuevas += avisar(TipoAlerta.CORREO_BLOQUEADO, correo, "El correo " + correo + " tuvo "
                    + IntentosAcceso.MAXIMO_FALLOS + " o más inicios de sesión fallidos seguidos y quedó bloqueado "
                    + IntentosAcceso.BLOQUEO.toMinutes() + " minutos.", ahora);
        for (String usuario : auditoria.usuariosConAccesosDenegados(desde, ACCESOS_DENEGADOS))
            nuevas += avisar(TipoAlerta.ACCESOS_DENEGADOS, usuario, "El usuario " + usuario + " intentó "
                    + ACCESOS_DENEGADOS + " o más operaciones para las que no tiene permiso en los últimos "
                    + VENTANA.toMinutes() + " minutos.", ahora);
        for (String sesion : auditoria.sesionesRechazadas(desde))
            nuevas += avisar(TipoAlerta.SESION_CERRADA_USADA, sesion, "Se intentó usar la sesión " + sesion
                    + ", que ya estaba cerrada, era de un usuario inactivo o era anterior a un cambio de contraseña."
                    + " Si el usuario no lo reconoce, su token pudo ser robado.", ahora);
        return nuevas;
    }

    @Override
    public boolean verificarIntegridad() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        VerificacionCadena cadena = auditoria.verificarCadena();
        if (!cadena.integra())
            avisar(TipoAlerta.AUDITORIA_ALTERADA, "cadena", "La cadena de la auditoría no coincide: "
                    + cadena.falla() + ". Alguien modificó o borró registros directamente en la base de datos.", ahora);
        List<String> alterados = archivos.verificarHuellas();
        for (String archivo : alterados)
            avisar(TipoAlerta.ARCHIVO_ALTERADO, archivo, "El archivo " + archivo
                    + " no coincide con la huella que se le calculó al guardarlo.", ahora);
        return cadena.integra() && alterados.isEmpty();
    }

    private int avisar(TipoAlerta tipo, String clave, String detalle, LocalDateTime fecha) {
        return alertas.alertar(new AlertaSeguridad(tipo, clave, detalle, fecha)) ? 1 : 0;
    }
}
