package com.sgtg.backend.infraestructure.configuration;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sgtg.backend.application.input.auth.ManageAuthCUIntPort;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.domain.use_cases.ManageAuthCUImplAdapter;
import com.sgtg.backend.domain.use_cases.ManageUsuarioCUImplAdapter;
import com.sgtg.backend.application.input.usuario.ManageUsuarioCUIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.application.input.calendario.ManageCalendarioCUIntPort;
import com.sgtg.backend.application.input.catalogo.ManageCatalogoCUIntPort;
import com.sgtg.backend.application.input.perfil.ManagePerfilCUIntPort;
import com.sgtg.backend.application.output.ManageCalendarioGatewayIntPort;
import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.domain.use_cases.ManageCalendarioCUImplAdapter;
import com.sgtg.backend.domain.use_cases.ManageCatalogoCUImplAdapter;
import com.sgtg.backend.domain.use_cases.ManagePerfilCUImplAdapter;
import com.sgtg.backend.application.input.trabajo_grado.ManageTrabajoGradoCUIntPort;
import com.sgtg.backend.application.output.ManageTrabajoGradoGatewayIntPort;
import com.sgtg.backend.application.output.ManageArchivoGatewayIntPort;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.use_cases.ManageTrabajoGradoCUImplAdapter;
import com.sgtg.backend.infraestructure.output.auth.AuthService;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public ManageAuthCUIntPort manageAuthCUIntPort(ManageAuthServiceGatewayIntPort authServiceGateway,
            ManageAuthGatewayIntPort persistenceAuthService, ExceptionFormatterIntPort exceptionFormatter) {
        return new ManageAuthCUImplAdapter(authServiceGateway, persistenceAuthService, exceptionFormatter);
    }

    @Bean
    public ManageUsuarioCUIntPort manageUsuarioCUIntPort(ManageUserGatewayIntPort userGateway,
            ManageAuthGatewayIntPort authGateway, ManageAuthServiceGatewayIntPort authServiceGateway,
            ManagePerfilGatewayIntPort perfilGateway, ManageCatalogoGatewayIntPort catalogoGateway,
            ExceptionFormatterIntPort exceptionFormatter) {
        return new ManageUsuarioCUImplAdapter(userGateway, authGateway, authServiceGateway, perfilGateway,
                catalogoGateway, exceptionFormatter);
    }

    /**
     * Reloj de la aplicación en la zona horaria de Colombia. Los casos de uso lo
     * usan para obtener la fecha actual que pasan al dominio. Solo se puede
     * adelantar en desarrollo ({@code sgtg.reloj.ajustable=true}).
     */
    @Bean
    public RelojAjustable clock() {
        return new RelojAjustable(Clock.system(ZoneId.of("America/Bogota")));
    }

    @Bean
    public ManageCatalogoCUIntPort manageCatalogoCUIntPort(ManageCatalogoGatewayIntPort catalogoGateway,
            ExceptionFormatterIntPort exceptionFormatter) {
        return new ManageCatalogoCUImplAdapter(catalogoGateway, exceptionFormatter);
    }

    @Bean
    public ManagePerfilCUIntPort managePerfilCUIntPort(ManagePerfilGatewayIntPort perfilGateway,
            ManageCatalogoGatewayIntPort catalogoGateway, ManageUserGatewayIntPort userGateway,
            ExceptionFormatterIntPort exceptionFormatter) {
        return new ManagePerfilCUImplAdapter(perfilGateway, catalogoGateway, userGateway, exceptionFormatter);
    }

    @Bean
    public ManageCalendarioCUIntPort manageCalendarioCUIntPort(ManageCalendarioGatewayIntPort calendarioGateway,
            ExceptionFormatterIntPort exceptionFormatter) {
        return new ManageCalendarioCUImplAdapter(calendarioGateway, exceptionFormatter);
    }

    @Bean
    public ManageTrabajoGradoCUIntPort manageTrabajoGradoCUIntPort(ManageTrabajoGradoGatewayIntPort trabajoGateway,
            ManageUserGatewayIntPort userGateway, ManagePerfilGatewayIntPort perfilGateway,
            ManageArchivoGatewayIntPort archivoGateway, CalendarioHabil calendario, Clock clock,
            ExceptionFormatterIntPort exceptionFormatter) {
        return new ManageTrabajoGradoCUImplAdapter(trabajoGateway, userGateway, perfilGateway, archivoGateway,
                calendario, clock, exceptionFormatter);
    }

}
