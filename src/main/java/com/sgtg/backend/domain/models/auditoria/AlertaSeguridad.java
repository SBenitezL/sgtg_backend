package com.sgtg.backend.domain.models.auditoria;

import java.time.LocalDateTime;

/**
 * Alerta de seguridad para los administradores.
 *
 * @param tipo    qué se detectó
 * @param clave   a quién o a qué se refiere (una IP, un correo, una sesión, una ruta): con el tipo, evita
 *                repetir el aviso del mismo hecho
 * @param detalle explicación para el administrador
 * @param fecha   cuándo se detectó (hora real del servidor)
 */
public record AlertaSeguridad(TipoAlerta tipo, String clave, String detalle, LocalDateTime fecha) {
}
