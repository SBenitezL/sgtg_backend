package com.sgtg.backend.application.input.usuario;

import java.util.List;

import com.sgtg.backend.domain.models.Usuario;

/** Puerto de entrada para la administración de usuarios. */
public interface ManageUsuarioCUIntPort {

    /**
     * Crea un usuario con sus roles y, si se indica, su perfil académico. Solo lo
     * hace un administrador: no existe registro público. El perfil se valida
     * antes de crear el usuario, para no dejar usuarios a medias.
     *
     * @param datos datos del usuario, sus roles y su perfil
     * @return el usuario creado con su identificador y sus perfiles
     */
    public FichaUsuario crear(NuevoUsuario datos);

    /**
     * Crea un usuario a nombre de un gestor. El administrador crea cualquiera;
     * Comité, jefe de departamento, Consejo y Decanatura solo con los roles
     * Estudiante y Docente, y el Comité y el jefe solo con un perfil de su
     * departamento (docente) o de uno de sus programas (estudiante).
     *
     * @param gestorId usuario autenticado que crea
     */
    public FichaUsuario crear(Long gestorId, NuevoUsuario datos);

    /**
     * Actualiza los datos, los roles y el perfil académico de un usuario. Una
     * contraseña vacía conserva la actual. Lo hace el administrador.
     *
     * @param usuarioId usuario a actualizar
     * @param datos     nuevos datos
     * @return el usuario actualizado con sus perfiles
     */
    public FichaUsuario actualizar(Long usuarioId, NuevoUsuario datos);

    /**
     * Actualiza un usuario a nombre de un gestor: si no es el administrador, el
     * usuario debe estar en su alcance y no tener roles administrativos, solo se
     * asignan o quitan los roles Estudiante y Docente y el perfil sigue en su
     * alcance.
     *
     * @param gestorId usuario autenticado que actualiza
     */
    public FichaUsuario actualizar(Long gestorId, Long usuarioId, NuevoUsuario datos);

    /**
     * Consulta un usuario por su identificador.
     *
     * @param usuarioId identificador del usuario
     * @return el usuario encontrado
     */
    public Usuario consultar(Long usuarioId);

    /**
     * Consulta un usuario con sus perfiles académicos.
     *
     * @param usuarioId identificador del usuario
     * @return el usuario encontrado con sus perfiles
     */
    public FichaUsuario consultarFicha(Long usuarioId);

    /**
     * Consulta un usuario del alcance del gestor (ver {@link #listarGestionados}).
     *
     * @param gestorId usuario autenticado que consulta
     */
    public FichaUsuario consultarFicha(Long gestorId, Long usuarioId);

    /**
     * Usuarios que gestiona el gestor, con si puede modificarlos. El
     * administrador, el Consejo y Decanatura gestionan a todos los usuarios; el
     * Comité y el jefe de departamento, a los docentes de su departamento y a
     * los estudiantes de los programas de ese departamento. A los usuarios con
     * roles administrativos solo los modifica el administrador.
     *
     * @param gestorId usuario autenticado
     */
    public List<UsuarioGestionado> listarGestionados(Long gestorId);

    /** @return todos los usuarios con sus perfiles, en orden de creación */
    public List<FichaUsuario> listar();

    /**
     * Reemplaza los roles de un usuario. Lo usa el administrador para asignar los
     * roles que no se pueden elegir en el registro público (comité, consejo,
     * jefe de departamento, etc.).
     *
     * @param usuarioId identificador del usuario
     * @param roles     nombres de los roles a asignar, al menos uno
     * @return el usuario con sus nuevos roles
     */
    public FichaUsuario asignarRoles(Long usuarioId, List<String> roles);

    /**
     * Reemplaza los roles de un usuario a nombre de un gestor, con las mismas
     * reglas de {@link #actualizar(Long, Long, NuevoUsuario)}.
     *
     * @param gestorId usuario autenticado que asigna
     */
    public FichaUsuario asignarRoles(Long gestorId, Long usuarioId, List<String> roles);

    /**
     * Cambia la contraseña del usuario autenticado. Exige la contraseña actual;
     * la nueva debe tener al menos {@link Usuario#LONGITUD_MINIMA_PASSWORD}
     * caracteres y ser distinta de la actual.
     *
     * @param usuarioId      usuario autenticado
     * @param passwordActual contraseña actual
     * @param passwordNueva  contraseña nueva
     */
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva);

    /**
     * Activa o inactiva un usuario. Un usuario inactivo no puede iniciar sesión
     * y sus tokens dejan de ser válidos. Nadie puede inactivarse a sí mismo; un
     * gestor que no es el administrador solo cambia el estado de los usuarios de
     * su alcance sin roles administrativos.
     *
     * @param administradorId usuario autenticado que hace el cambio (el gestor)
     * @param usuarioId       usuario a modificar
     * @param activo          nuevo estado
     * @return el usuario con su nuevo estado
     */
    public FichaUsuario cambiarEstado(Long administradorId, Long usuarioId, boolean activo);
}
