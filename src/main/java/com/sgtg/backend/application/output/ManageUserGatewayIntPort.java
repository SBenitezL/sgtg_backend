package com.sgtg.backend.application.output;

import com.sgtg.backend.domain.models.Usuario;

/**
 * Puerto de salida para consultar y actualizar los usuarios del sistema.
 */
public interface ManageUserGatewayIntPort {

    /**
     * @param email correo del usuario
     * @return el usuario, o null si no existe
     */
    public Usuario findByEmail(String email);

    /**
     * @param usuarioId identificador del usuario
     * @return el usuario, o null si no existe
     */
    public Usuario findById(Long usuarioId);

    /**
     * Reemplaza los roles del usuario por los que tiene el objeto de dominio.
     *
     * @param usuario usuario con sus roles ya cargados (con identificador)
     */
    public void actualizarRoles(Usuario usuario);
}
