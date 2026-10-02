package com.sgtg.backend.infraestructure.configuration;

import com.sgtg.backend.infraestructure.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad de la API: stateless con JWT. Las rutas de autenticación son
 * públicas; el resto requiere token. Los permisos por rol se declaran en los
 * controladores con {@code @PreAuthorize} (habilitado por
 * {@link EnableMethodSecurity}).
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final AuthenticationProvider authProvider;

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/sgtg/auth/**").permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(
                                                sessionManager -> sessionManager
                                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Sin token válido: 401 (no autenticado); sin permiso: 403
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                                .authenticationProvider(authProvider)
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }
}
