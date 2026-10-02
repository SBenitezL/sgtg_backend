package com.sgtg.backend.application.input.usuario;

import java.util.List;

import com.sgtg.backend.domain.models.Usuario;

/** Puerto de entrada para la administración de usuarios. */
public interface ManageUsuarioCUIntPort {

    /**
     * Crea un usuario con sus roles. Solo lo hace un administrador: no existe
     * registro público.
     *
     * @param datos datos del usuario y sus roles
     * @return el usuario creado con su identificador
     */
    public Usuario crear(NuevoUsuario datos);

    /**
     * Consulta un usuario por su identificador.
     *
     * @param usuarioId identificador del usuario
     * @return el usuario encontrado
     */
    public Usuario consultar(Long usuarioId);

    /**
     * Reemplaza los roles de un usuario. Lo usa el administrador para asignar los
     * roles que no se pueden elegir en el registro público (comité, consejo,
     * jefe de departamento, etc.).
     *
     * @param usuarioId identificador del usuario
     * @param roles     nombres de los roles a asignar, al menos uno
     * @return el usuario con sus nuevos roles
     */
    public Usuario asignarRoles(Long usuarioId, List<String> roles);
}
