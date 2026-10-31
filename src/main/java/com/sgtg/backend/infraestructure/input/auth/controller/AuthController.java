package com.sgtg.backend.infraestructure.input.auth.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.auth.ManageAuthCUIntPort;
import com.sgtg.backend.application.input.auth.TokensSesion;
import com.sgtg.backend.infraestructure.input.auditoria.AuditoriaFilter;
import com.sgtg.backend.infraestructure.input.auth.dto.input.LoginDTORequest;
import com.sgtg.backend.infraestructure.input.auth.dto.output.AuthDTOResponse;
import com.sgtg.backend.infraestructure.jwt.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Autenticación pública. No hay registro público: los usuarios los crea un
 * administrador en {@code POST /api/v1/sgtg/usuarios}.
 * <p>
 * El token de acceso va en el cuerpo de la respuesta y dura poco; el de
 * renovación va en la cookie {@value #COOKIE}: {@code HttpOnly} (el navegador
 * no deja leerla desde JavaScript), {@code Secure}, {@code SameSite=Strict} y
 * solo para {@value #RUTA_COOKIE}, así que no viaja en las demás peticiones.
 */
@RestController
@RequestMapping("/api/v1/sgtg/auth/")
public class AuthController {

    public static final String COOKIE = "sgtg_renovacion";
    static final String RUTA_COOKIE = "/api/v1/sgtg/auth";
    /**
     * Cabecera obligatoria para renovar: un formulario de otro sitio no puede
     * enviarla y un script de otro sitio necesitaría un permiso CORS que no hay
     */
    static final String CABECERA_RENOVAR = "X-Requested-With";
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final ManageAuthCUIntPort manageAuth;
    /** Solo se desactiva para servir por HTTP fuera de localhost (nunca en producción) */
    private final boolean cookieSegura;

    public AuthController(ManageAuthCUIntPort manageAuth,
            @Value("${sgtg.sesion.cookie-segura:true}") boolean cookieSegura) {
        this.manageAuth = manageAuth;
        this.cookieSegura = cookieSegura;
    }

    /**
     * Inicia sesión: devuelve el token de acceso y deja el de renovación en la
     * cookie. Credenciales incorrectas, usuario inactivo o correo bloqueado por
     * intentos fallidos: 401 con el mensaje correspondiente. La sesión nueva
     * queda en la auditoría.
     */
    @PostMapping("login")
    public ResponseEntity<AuthDTOResponse> login(@Valid @RequestBody LoginDTORequest loginRequest,
            HttpServletRequest request) {
        return conSesion(manageAuth.login(loginRequest.getEmail(), loginRequest.getPassword()), request);
    }

    /**
     * Renueva la sesión de la cookie: token de acceso nuevo y cookie nueva (la
     * anterior deja de servir). Sin la cabecera {@value #CABECERA_RENOVAR}: 403.
     * Sesión cerrada, vencida o token reutilizado: 401.
     */
    @PostMapping("renovar")
    public ResponseEntity<AuthDTOResponse> renovar(
            @CookieValue(name = COOKIE, required = false) String tokenRenovacion,
            @RequestHeader(name = CABECERA_RENOVAR, required = false) String cabecera, HttpServletRequest request) {
        if (cabecera == null || cabecera.isBlank())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return conSesion(manageAuth.renovar(tokenRenovacion), request);
    }

    /**
     * Cierra la sesión del token de acceso (o, si ya venció, la de la cookie) y
     * borra la cookie. Responde 204 aunque no haya sesión o ya no sea válida.
     */
    @PostMapping("logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE, required = false) String tokenRenovacion,
            HttpServletRequest request) {
        manageAuth.logout(JwtAuthenticationFilter.tokenDe(request), tokenRenovacion);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    private ResponseEntity<AuthDTOResponse> conSesion(TokensSesion tokens, HttpServletRequest request) {
        request.setAttribute(AuditoriaFilter.ATRIBUTO_SESION, tokens.sesion());
        Duration vida = Duration.between(LocalDateTime.now(ZONA), tokens.expiraRenovacion());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(tokens.tokenRenovacion(), vida).toString())
                .body(new AuthDTOResponse(tokens.tokenAcceso()));
    }

    private ResponseCookie cookie(String valor, Duration vida) {
        return ResponseCookie.from(COOKIE, valor).httpOnly(true).secure(cookieSegura).sameSite("Strict")
                .path(RUTA_COOKIE).maxAge(vida.isNegative() ? Duration.ZERO : vida).build();
    }
}
