package com.sgtg.backend.domain.use_cases;

import com.sgtg.backend.application.input.auth.ManageAuthCUIntPort;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.domain.models.Usuario;

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

    @Override
    public String login(String email, String password) {
        Usuario user = persistenceAuthService.findByEmail(email);
        if (user == null)
            this.exceptionFormatter.returnNoData("error.auth.user_not_found");
        String token = this.authServiceGateway.login(user.getEmail(), password);
        if (token == null)
            this.exceptionFormatter.returnResponseBadCredentials("error.auth.invalid_credentials");
        return token;
    }

    @Override
    public boolean register(Usuario usuario) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'register'");
    }

}
