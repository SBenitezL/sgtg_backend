package com.sgtg.backend.infraestructure.configuration;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Pasa las contraseñas de las bases creadas antes de separar las credenciales:
 * copia los hashes de {@code users.password} a {@code credenciales} y borra esa
 * columna (con {@code ddl-auto=update} Hibernate no la borra, y como es
 * obligatoria impediría crear usuarios). Si la columna ya no existe no hace
 * nada. Se ejecuta al terminar de crear los beans, antes de que el servidor
 * reciba peticiones.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigracionCredenciales implements SmartInitializingSingleton {

    private final JdbcTemplate jdbc;

    @Override
    public void afterSingletonsInstantiated() {
        Integer columnas = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns"
                + " WHERE table_schema = DATABASE() AND LOWER(table_name) = 'users'"
                + " AND LOWER(column_name) = 'password'", Integer.class);
        if (columnas == null || columnas == 0)
            return;
        int copiadas = jdbc.update("INSERT INTO credenciales (usuario_id, password_hash, fecha_cambio, debe_cambiar, version)"
                + " SELECT u.cusuari, u.password, NOW(), FALSE, 0 FROM users u"
                + " WHERE NOT EXISTS (SELECT 1 FROM credenciales c WHERE c.usuario_id = u.cusuari)");
        jdbc.execute("ALTER TABLE users DROP COLUMN password");
        log.info("Contraseñas pasadas a la tabla credenciales: {}; se borró la columna users.password", copiadas);
    }
}
