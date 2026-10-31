package com.sgtg.backend.infraestructure.configuration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * En MySQL, crea triggers que rechazan cualquier {@code UPDATE} o
 * {@code DELETE} sobre {@code auditoria}, la haga quien la haga: ni el usuario
 * de la aplicación ni alguien con sus credenciales puede alterar los
 * registros. Para borrar registros antiguos, un administrador de la base debe
 * quitar los triggers a propósito (queda la evidencia de que lo hizo, y la
 * cadena de sellos lo detecta).
 * <p>
 * Crear triggers requiere permisos de esquema: los crea quien también maneja
 * el esquema ({@code ddl-auto=update}): el paso de migración en producción y la
 * aplicación en desarrollo. Con {@code ddl-auto=validate} (el usuario de la
 * aplicación en producción, que ni siquiera puede ver los triggers) no hace
 * nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProteccionAuditoria implements SmartInitializingSingleton {

    static final String SIN_CAMBIOS = "auditoria_sin_cambios";
    static final String SIN_BORRADOS = "auditoria_sin_borrados";

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String ddlAuto;

    @Override
    public void afterSingletonsInstantiated() {
        if (!"update".equalsIgnoreCase(ddlAuto) || !esMySql())
            return;
        // Sin tildes: el mensaje lo muestra cualquier cliente de MySQL, con cualquier juego de caracteres
        crear(SIN_CAMBIOS, "UPDATE", "La auditoria no se puede modificar");
        crear(SIN_BORRADOS, "DELETE", "La auditoria no se puede borrar");
    }

    private void crear(String nombre, String operacion, String mensaje) {
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.triggers"
                + " WHERE trigger_schema = DATABASE() AND trigger_name = ?", Integer.class, nombre);
        if (existe != null && existe > 0)
            return;
        try {
            jdbc.execute("CREATE TRIGGER " + nombre + " BEFORE " + operacion + " ON auditoria FOR EACH ROW"
                    + " SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '" + mensaje + "'");
            log.info("Protección de la auditoría: se creó el trigger {}", nombre);
        } catch (DataAccessException e) {
            log.warn("No se pudo crear el trigger {} de la auditoría: este usuario no tiene permiso TRIGGER", nombre);
        }
    }

    private boolean esMySql() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql");
        } catch (SQLException e) {
            return false;
        }
    }
}
