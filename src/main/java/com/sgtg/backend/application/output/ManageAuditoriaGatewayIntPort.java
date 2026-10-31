package com.sgtg.backend.application.output;

import java.time.LocalDateTime;
import java.util.List;

import com.sgtg.backend.domain.models.auditoria.RegistroAuditoria;
import com.sgtg.backend.domain.models.auditoria.VerificacionCadena;

/**
 * Almacén de la auditoría. Solo agrega registros, sellados en una cadena: la
 * aplicación no los modifica ni los borra, y no hay ninguna pantalla ni
 * endpoint para consultarlos. Las consultas de este puerto solo las usan la
 * detección de alertas y la verificación de la cadena.
 */
public interface ManageAuditoriaGatewayIntPort {

    /** Guarda el registro sellado a continuación del último de la cadena. */
    void registrar(RegistroAuditoria registro);

    /** IPs con al menos {@code minimo} inicios de sesión fallidos desde {@code desde}. */
    List<String> ipsConLoginsFallidos(LocalDateTime desde, long minimo);

    /** Correos con al menos {@code minimo} inicios de sesión fallidos desde {@code desde}. */
    List<String> correosConLoginsFallidos(LocalDateTime desde, long minimo);

    /** Usuarios (correo) con al menos {@code minimo} operaciones sin permiso (403) desde {@code desde}. */
    List<String> usuariosConAccesosDenegados(LocalDateTime desde, long minimo);

    /**
     * Sesiones cuyo token, con firma válida y sin vencer, se rechazó desde
     * {@code desde}: sesión cerrada, usuario inactivo o token anterior a un
     * cambio de contraseña.
     */
    List<String> sesionesRechazadas(LocalDateTime desde);

    /** Recorre la cadena desde el primer registro sellado recalculando cada sello. */
    VerificacionCadena verificarCadena();
}
