package com.sgtg.backend.infraestructure.configuration;

import java.time.Clock;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgtg.backend.application.output.ManageAuditoriaGatewayIntPort;
import com.sgtg.backend.infraestructure.input.auditoria.AuditoriaFilter;
import com.sgtg.backend.infraestructure.jwt.JwtAuthenticationFilter;
import com.sgtg.backend.infraestructure.output.exception.structure.Error;
import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;
import com.sgtg.backend.infraestructure.output.exception.structure.ErrorUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad de la API: stateless con JWT. Las rutas de autenticación son
 * públicas; el resto requiere token. Los permisos por rol se declaran en los
 * controladores con {@code @PreAuthorize} (habilitado por
 * {@link EnableMethodSecurity}). Después del filtro JWT va la auditoría
 * ({@link AuditoriaFilter}), que registra las operaciones con su usuario.
 * <p>
 * Un usuario que debe cambiar la contraseña que le asignó un administrador solo
 * puede consultar sus datos y cambiarla; lo demás responde 403 con el código
 * {@code GC-0012}.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private static final String API = "/api/v1/sgtg";

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final ManageAuditoriaGatewayIntPort auditoria;
        private final ObjectMapper json;
        private final MessageSource mensajes;

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth.requestMatchers(API + "/auth/**").permitAll()
                                                // Lo único que puede hacer quien debe cambiar la contraseña
                                                .requestMatchers(HttpMethod.GET, API + "/usuarios/me").authenticated()
                                                .requestMatchers(HttpMethod.PUT, API + "/usuarios/me/password")
                                                .authenticated()
                                                .anyRequest().access(AuthorizationManagers.allOf(
                                                                AuthenticatedAuthorizationManager.authenticated(),
                                                                AuthorizationManagers.not(AuthorityAuthorizationManager
                                                                                .hasAuthority(JwtAuthenticationFilter.CAMBIO_PASSWORD_PENDIENTE)))))
                                .sessionManagement(
                                                sessionManager -> sessionManager
                                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Sin token válido: 401 (no autenticado); sin permiso: 403
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                                                .accessDeniedHandler(sinPermiso()))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                // Con el reloj real: la auditoría no usa el reloj ajustable de desarrollo
                                .addFilterAfter(new AuditoriaFilter(auditoria, json,
                                                Clock.system(ZoneId.of("America/Bogota"))), JwtAuthenticationFilter.class);
                return http.build();
        }

        /**
         * 403 de las rutas: si el usuario debe cambiar la contraseña, con el código
         * {@code GC-0012} y el mensaje, para que el front lo lleve a cambiarla.
         */
        private AccessDeniedHandler sinPermiso() {
                AccessDeniedHandler porDefecto = new AccessDeniedHandlerImpl();
                return (request, response, ex) -> {
                        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
                        boolean pendiente = autenticacion != null && autenticacion.getAuthorities().stream()
                                        .anyMatch(p -> JwtAuthenticationFilter.CAMBIO_PASSWORD_PENDIENTE
                                                        .equals(p.getAuthority()));
                        if (!pendiente) {
                                porDefecto.handle(request, response, ex);
                                return;
                        }
                        Error error = ErrorUtils.createError(ErrorCode.PASSWORD_CHANGE_REQUIRED.getCode(),
                                        mensajes.getMessage("error.auth.debe_cambiar_password", null,
                                                        request.getLocale()),
                                        HttpStatus.FORBIDDEN.value())
                                        .setUrl(request.getRequestURL().toString()).setMethod(request.getMethod());
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        json.writeValue(response.getOutputStream(), error);
                };
        }
}
