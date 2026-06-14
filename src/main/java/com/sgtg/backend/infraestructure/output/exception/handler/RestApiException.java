package com.sgtg.backend.infraestructure.output.exception.handler;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sgtg.backend.infraestructure.output.exception.own.BadCredentialException;
import com.sgtg.backend.infraestructure.output.exception.own.BadFormatException;
import com.sgtg.backend.infraestructure.output.exception.own.BusinessRuleException;
import com.sgtg.backend.infraestructure.output.exception.own.EntityExistsException;
import com.sgtg.backend.infraestructure.output.exception.own.EntityNotFoundException;
import com.sgtg.backend.infraestructure.output.exception.own.NoAccessException;
import com.sgtg.backend.infraestructure.output.exception.own.NoDataException;
import com.sgtg.backend.infraestructure.output.exception.structure.Error;
import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;
import com.sgtg.backend.infraestructure.output.exception.structure.ErrorUtils;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class RestApiException {
    /**
     * Manejador global para excepciones no controladas, captura cualquier excepción
     * que no haya sido manejada
     * específicamente y devuelve una respuesta de error genérica con detalles del
     * error.
     * 
     * @param req    El objeto HttpServletRequest que contiene detalles de la
     *               solicitud que causó la excepción.
     * @param ex     La excepción que fue lanzada.
     * @param locale El objeto Locale que representa la configuración regional del
     *               cliente, utilizado para localizar mensajes de error.
     * @return Una ResponseEntity que contiene un objeto Error con detalles del
     *         error y un código de estado HTTP 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleException(final HttpServletRequest req,
            final Exception ex, final Locale locale) {
        final Error error = ErrorUtils.createError(ErrorCode.GENERIC_ERROR.getCode(),
                ErrorCode.GENERIC_ERROR.getMessageKey(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()).setUrl(req.getRequestURL().toString())
                .setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Manejador específico para la excepción ExpiredJwtException, que se lanza
     * cuando un token JWT ha expirado. Este método captura la excepción, crea un
     * objeto Error con detalles específicos sobre el error de token expirado y
     * devuelve una respuesta con un código de estado HTTP 401 (Unauthorized).
     * 
     * @param req    El objeto HttpServletRequest que contiene detalles de la
     *               solicitud que causó la excepción.
     * @param ex     La excepción ExpiredJwtException que fue lanzada.
     * @param locale El objeto Locale que representa la configuración regional del
     *               cliente, utilizado para localizar mensajes de error.
     * @return
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Error> handleExpiredJwtException(final HttpServletRequest req,
            final ExpiredJwtException ex, final Locale locale) {
        final Error error = ErrorUtils
                .createError(ErrorCode.TOKEN_EXPIRED.getCode(),
                        ErrorCode.TOKEN_EXPIRED.getMessageKey(),
                        HttpStatus.UNAUTHORIZED.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Manejador específico para la excepción AccessDeniedException, que se lanza
     * cuando un usuario intenta acceder a un recurso para el cual no tiene
     * permisos.
     * 
     * @param req    El objeto HttpServletRequest que contiene detalles de la
     *               solicitud que causó la excepción.
     * @param ex     La excepción AccessDeniedException que fue lanzada.
     * @param locale El objeto Locale que representa la configuración regional del
     *               cliente, utilizado para localizar mensajes de error.
     * @return
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Error> handleAccessDeniedException(final HttpServletRequest req,
            final AccessDeniedException ex, final Locale locale) {
        final Error error = ErrorUtils
                .createError(ErrorCode.NO_ACCESS.getCode(),
                        ErrorCode.NO_ACCESS.getMessageKey(),
                        HttpStatus.FORBIDDEN.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Manejador específico para la excepción EntityExistsException, que se lanza
     * cuando se intenta crear una entidad que ya existe en la base de datos. Este
     * método captura la excepción, crea un objeto Error con detalles específicos
     * sobre el error de entidad existente y devuelve una respuesta con un código de
     * estado HTTP 406 (Not Acceptable).
     * 
     * @param req    El objeto HttpServletRequest que contiene detalles de la
     *               solicitud que causó la excepción.
     * @param ex     La excepción EntityExistsException que fue lanzada.
     * @param locale El objeto Locale que representa la configuración regional del
     *               cliente, utilizado para localizar mensajes de error.
     * @return Una ResponseEntity que contiene un objeto Error con detalles del
     *         error y un código de estado HTTP 406 (Not Acceptable).
     */
    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<Error> handleGenericException(final HttpServletRequest req,
            final EntityExistsException ex) {
        final Error error = ErrorUtils
                .createError(ErrorCode.ENTITY_EXISTS.getCode(),
                        String.format("%s, %s", ErrorCode.ENTITY_EXISTS.getMessageKey(),
                                ex.getMessage()),
                        HttpStatus.NOT_ACCEPTABLE.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    /**
     * Manejador específico para la excepción NoDataException, que se lanza cuando
     * no se encuentran datos para una consulta o solicitud. Este método captura la
     * excepción, crea un objeto Error con
     * 
     * @param req El objeto HttpServletRequest que contiene detalles de la solicitud
     *            que causó la excepción.
     * @param ex  La excepción NoDataException que fue lanzada.
     * @return Una ResponseEntity que contiene un objeto Error con detalles del
     *         error y un código de estado HTTP 406 (Not Acceptable).
     */
    @ExceptionHandler(NoDataException.class)
    public ResponseEntity<Error> handleNoDataException(final HttpServletRequest req,
            final NoDataException ex) {
        final Error error = ErrorUtils
                .createError(ErrorCode.NO_DATA.getCode(),
                        String.format("%s, %s", ErrorCode.NO_DATA.getMessageKey(),
                                ex.getMessage()),
                        HttpStatus.NOT_ACCEPTABLE.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    /**
     * Manejador específico para la excepción EntityNotFoundException, que se lanza
     * cuando no se encuentra una entidad en la base de datos. Este método captura
     * la excepción, crea un objeto Error con detalles específicos sobre el error de
     * entidad
     * 
     * @param req    El objeto HttpServletRequest que contiene detalles de la
     *               solicitud que causó la excepción.
     * @param ex     La excepción EntityNotFoundException que fue lanzada.
     * @param locale El objeto Locale que representa la configuración regional del
     *               cliente, utilizado para localizar mensajes de error.
     * @return Una ResponseEntity que contiene un objeto Error con detalles del
     *         error y un código de estado HTTP 404 (Not Found).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Error> handleNotFoundException(final HttpServletRequest req,
            final EntityNotFoundException ex, final Locale locale) {
        final Error error = ErrorUtils
                .createError(ErrorCode.ENTITY_NOT_FOUND.getCode(),
                        String.format("%s, %s",
                                ErrorCode.ENTITY_NOT_FOUND.getMessageKey(),
                                ex.getMessage()),
                        HttpStatus.NOT_FOUND.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Manejador específico para la excepción BadCredentialsException, que se lanza
     * cuando las credenciales proporcionadas para la autenticación son incorrectas.
     * Este método captura la excepción, crea un objeto Error con detalles
     * específicos sobre el error de credenciales incorrectas y devuelve una
     * respuesta con un código de estado HTTP 404 (Not Found).
     * 
     * @param req    El objeto HttpServletRequest que contiene detalles de la
     *               solicitud que causó la excepción.
     * @param ex     La excepción BadCredentialsException que fue lanzada.
     * @param locale El objeto Locale que representa la configuración regional del
     *               cliente, utilizado para localizar mensajes de error.
     * @return La respuesta HTTP con el error correspondiente.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Error> handleBadCredentialsException(final HttpServletRequest req,
            final BadCredentialsException ex, final Locale locale) {
        final Error error = ErrorUtils
                .createError(ErrorCode.BAD_CREDENTIALS.getCode(),
                        String.format("%s, %s",
                                ErrorCode.BAD_CREDENTIALS.getMessageKey(),
                                ex.getMessage()),
                        HttpStatus.NOT_FOUND.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Manejador específico para la excepción BadCredentialException, que se lanza
     * cuando las credenciales proporcionadas para la autenticación son incorrectas.
     * 
     * @param req    El objeto HttpServletRequest que contiene detalles de la
     *               solicitud que causó la excepción.
     * @param ex     La excepción BadCredentialException que fue lanzada.
     * @param locale El objeto Locale que representa la configuración regional del
     *               cliente, utilizado para localizar mensajes de error.
     * @return Una ResponseEntity que contiene un objeto Error con detalles del
     *         error y un código de estado HTTP 404 (Not Found).
     */
    @ExceptionHandler(BadCredentialException.class)
    public ResponseEntity<Error> handleBadCredentialException(final HttpServletRequest req,
            final BadCredentialException ex, final Locale locale) {
        final Error error = ErrorUtils
                .createError(ErrorCode.BAD_CREDENTIALS.getCode(),
                        String.format("%s, %s",
                                ErrorCode.BAD_CREDENTIALS.getMessageKey(),
                                ex.getMessage()),
                        HttpStatus.NOT_FOUND.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Manejador específico para la excepción NoAccessException, que se lanza cuando
     * un usuario intenta acceder a un recurso para el cual no tiene permisos. Este
     * método captura la excepción, crea un objeto Error con detalles específicos
     * sobre el error de acceso denegado y devuelve una respuesta con un código de
     * estado HTTP 406 (Not Acceptable).
     * 
     * @param req    El objeto HttpServletRequest que contiene detalles de la
     *               solicitud que causó la excepción.
     * @param ex     La excepción NoAccessException que fue lanzada.
     * @param locale El objeto Locale que representa la configuración regional del
     *               cliente, utilizado para localizar mensajes de error.
     * @return Una ResponseEntity que contiene un objeto Error con detalles del
     *         error y un código de estado HTTP 406 (Not Acceptable).
     */
    @ExceptionHandler(NoAccessException.class)
    public ResponseEntity<Error> handleNoAccessException(final HttpServletRequest req,
            final NoAccessException ex, final Locale locale) {
        final Error error = ErrorUtils
                .createError(ErrorCode.NO_ACCESS.getCode(),
                        String.format("%s, %s",
                                ErrorCode.NO_ACCESS.getMessageKey(),
                                ex.getMessage()),
                        HttpStatus.NOT_ACCEPTABLE.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(BadFormatException.class)
    public ResponseEntity<Error> handleBadFormatException(final HttpServletRequest req,
            final BadFormatException ex, final Locale locale) {
        final Error error = ErrorUtils
                .createError(ErrorCode.BAD_FORMAT.getCode(),
                        String.format("%s, %s",
                                ErrorCode.BAD_FORMAT.getMessageKey(),
                                ex.getMessage()),
                        HttpStatus.NOT_ACCEPTABLE.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Error> handleBusinessRuleException(final HttpServletRequest req,
            final BusinessRuleException ex, final Locale locale) {
        final Error error = ErrorUtils
                .createError(ErrorCode.BUSINESS_RULE_VIOLATION.getCode(), ex.formatException(),
                        HttpStatus.BAD_REQUEST.value())
                .setUrl(req.getRequestURL().toString()).setMethod(req.getMethod());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String campo = ((FieldError) error).getField();
            String mensajeDeError = error.getDefaultMessage();
            errores.put(campo, mensajeDeError);
        });

        return new ResponseEntity<Map<String, String>>(errores, HttpStatus.BAD_REQUEST);
    }

}
