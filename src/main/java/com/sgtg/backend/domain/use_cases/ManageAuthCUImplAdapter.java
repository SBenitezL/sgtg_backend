package com.sgtg.backend.domain.use_cases;

import java.util.List;

import com.sgtg.backend.application.input.auth.ManageAuthCUIntPort;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
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
    public String register(Usuario usuario) {
        if (!this.saveUser(usuario))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.auth.register.user_already_exists");
        return this.login(usuario.getEmail(), usuario.getPassword());

    }

    private boolean saveUser(Usuario usuario) {
        if (this.persistenceAuthService.existsByEmailorCodigo(usuario.getEmail(), usuario.getCodigo()))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.auth.register.user_already_exists");
        if (!usuario.isValidRegister())
            this.exceptionFormatter
                    .returnResponseBusinessRuleViolated("error.auth.register.valid.user.business_rule_violated");
        List<Role> roles = this.persistenceAuthService
                .findRolesByTrole(usuario.getRole().stream().map(role -> role.getTrole()).toList());
        if (usuario.getRole().size() != roles.size())
            this.exceptionFormatter
                    .returnResponseBusinessRuleViolated("error.auth.register.valid.role.business_rule_violated");
        usuario.cargarRoles(roles);
        return this.authServiceGateway.register(usuario);
    }

}
