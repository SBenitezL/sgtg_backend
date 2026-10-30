package com.sgtg.backend.domain.use_cases;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import com.sgtg.backend.application.input.auth.ManageAuthCUIntPort;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.application.output.ManageIntentosAccesoGatewayIntPort;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.acceso.IntentosAcceso;
import com.sgtg.backend.domain.utils.CheckString;

public class ManageAuthCUImplAdapter implements ManageAuthCUIntPort {

    private final ManageAuthServiceGatewayIntPort authServiceGateway;
    private final ManageAuthGatewayIntPort persistenceAuthService;
    private final ManageIntentosAccesoGatewayIntPort intentosGateway;
    private final ExceptionFormatterIntPort exceptionFormatter;
    /** Reloj real: el bloqueo por intentos no usa el reloj ajustable de desarrollo */
    private final Clock reloj;

    public ManageAuthCUImplAdapter(ManageAuthServiceGatewayIntPort authServiceGateway,
            ManageAuthGatewayIntPort persistenceAuthService, ManageIntentosAccesoGatewayIntPort intentosGateway,
            ExceptionFormatterIntPort exceptionFormatter, Clock reloj) {
        this.authServiceGateway = authServiceGateway;
        this.persistenceAuthService = persistenceAuthService;
        this.intentosGateway = intentosGateway;
        this.exceptionFormatter = exceptionFormatter;
        this.reloj = reloj;
    }

    /**
     * Correo inexistente o contraseña incorrecta responden igual, para no revelar
     * qué correos están registrados. El estado del usuario se revisa solo después
     * de validar la contraseña: así solo su dueño sabe que la cuenta está inactiva.
     * <p>
     * Tras {@value IntentosAcceso#MAXIMO_FALLOS} fallos seguidos el correo queda
     * bloqueado un tiempo y se rechaza sin revisar la contraseña. El bloqueo se
     * lleva por correo exista o no la cuenta, así que tampoco revela cuáles existen.
     */
    @Override
    public String login(String email, String password) {
        if (CheckString.isBlank(email) || CheckString.isBlank(password))
            this.exceptionFormatter.returnResponseBadCredentials("error.auth.invalid_credentials");
        LocalDateTime ahora = LocalDateTime.now(reloj);
        IntentosAcceso intentos = intentosGateway.obtener(email.strip().toLowerCase(Locale.ROOT));
        if (intentos.estaBloqueado(ahora))
            this.exceptionFormatter.returnResponseBadCredentials("error.auth.acceso_bloqueado");
        if (!this.authServiceGateway.verificarCredenciales(email, password)) {
            intentos.registrarFallo(ahora);
            intentosGateway.guardar(intentos);
            this.exceptionFormatter.returnResponseBadCredentials(intentos.estaBloqueado(ahora)
                    ? "error.auth.acceso_bloqueado"
                    : "error.auth.invalid_credentials");
        }
        if (intentos.tieneRegistro())
            intentosGateway.eliminar(intentos.getCorreo());
        Usuario user = persistenceAuthService.findByEmail(email);
        if (!user.isActivo())
            this.exceptionFormatter.returnResponseBadCredentials("error.auth.usuario_inactivo");
        return this.authServiceGateway.generarToken(user.getEmail());
    }

    @Override
    public void logout(String token) {
        if (!CheckString.isBlank(token))
            this.authServiceGateway.cerrarSesion(token);
    }

    /** Los intentos fallidos de hace más de un día ya no cuentan para ningún bloqueo. */
    @Override
    public int depurarRegistrosDeAcceso() {
        return intentosGateway.depurar(LocalDateTime.now(reloj).minusDays(1))
                + this.authServiceGateway.depurarSesionesCerradas();
    }

}
