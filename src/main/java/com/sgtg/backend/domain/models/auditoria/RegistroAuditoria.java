package com.sgtg.backend.domain.models.auditoria;

import java.time.LocalDateTime;

/**
 * Registro de auditoría: quién hizo qué, cuándo, desde dónde y con qué
 * resultado. Solo se agrega (nunca se modifica ni se borra desde la
 * aplicación) y no se consulta desde el sistema: es evidencia para seguridad.
 *
 * @param fecha         fecha y hora real del servidor (no el reloj ajustable de desarrollo)
 * @param usuarioId     usuario autenticado, null si no lo hay (ej. un inicio de sesión fallido)
 * @param usuario       correo del usuario o, en un inicio de sesión, el correo con el que se intentó
 * @param roles         roles del usuario en ese momento, separados por comas
 * @param sesion        sesión (identificador del token) con la que se hizo, para unir todas las acciones de un
 *                      mismo inicio de sesión; null si la petición no traía token
 * @param ip           dirección IP del cliente
 * @param agenteUsuario navegador y sistema operativo (cabecera User-Agent)
 * @param metodo        método HTTP
 * @param ruta          ruta solicitada, con sus parámetros de consulta
 * @param operacion     operación del sistema que atendió la petición (ej. TrabajoGradoController.agendarReunion)
 * @param trabajoId     trabajo de grado afectado, null si la operación no es de un trabajo
 * @param parametros    variables de la ruta (ej. trabajoId=7, numero=2)
 * @param estadoHttp    código de respuesta
 * @param resultado     resultado según el código de respuesta
 */
public record RegistroAuditoria(LocalDateTime fecha, Long usuarioId, String usuario, String roles, String sesion, String ip,
        String agenteUsuario, String metodo, String ruta, String operacion, Long trabajoId, String parametros,
        int estadoHttp, ResultadoAuditoria resultado) {
}
