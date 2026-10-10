package com.sgtg.backend.domain.use_cases;

import com.sgtg.backend.application.input.auth.ManageAuthCUIntPort;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.utils.CheckString;

public class ManageAuthCUImplAdapter implements ManageAuthCUIntPort {

    private final ManageAuthServiceGatewayIntPort authServiceGateway;
    private final ManageAuthGatewayIntPort persistenceAuthService;
    private final ExceptionFormatterIntPort exceptionFormatter;

    public ManageAuthCUImplAdapter(ManageAuthServiceGatewayIntPort authServiceGateway,
            ManageAuthGatewayIntPort persistenceAuthService, ExceptionFormatterIntPort exceptionFormatter) {
        this.authServiceGateway = authServiceGateway;
        this.persistenceAuthService = persistenceAuthService;
        this.exceptionFormatter = exceptionFormatter;
    }

    /**
     * Correo inexistente o contraseña incorrecta responden igual, para no revelar
     * qué correos están registrados. El estado del usuario se revisa solo después
     * de validar la contraseña: así solo su dueño sabe que la cuenta está inactiva.
     */
    @Override
    public String login(String email, String password) {
        if (CheckString.isBlank(email) || CheckString.isBlank(password)
                || !this.authServiceGateway.verificarCredenciales(email, password))
            this.exceptionFormatter.returnResponseBadCredentials("error.auth.invalid_credentials");
        Usuario user = persistenceAuthService.findByEmail(email);
        if (!user.isActivo())
            this.exceptionFormatter.returnResponseBadCredentials("error.auth.usuario_inactivo");
        return this.authServiceGateway.generarToken(user.getEmail());
    }

}
