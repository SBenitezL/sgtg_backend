package com.sgtg.backend.domain.models.participantes;

import java.util.Set;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Rol que cumple una persona dentro de un trabajo de grado (estudiante,
 * director, evaluador o asesor). No hereda de {@link Usuario}: lo referencia
 * por su identificador y conserva una copia de los datos que necesita el
 * dominio, nunca las credenciales. Así una misma persona puede cumplir roles
 * distintos en distintos trabajos de grado.
 */
public abstract class Participante {
    /** Identificador del usuario del sistema, null solo para asesores sin cuenta */
    private Long usuarioId;
    /** Código con el que la persona se presenta frente a la Universidad */
    private String codigo;
    /** Nombres de la persona */
    private String nombres;
    /** Apellidos de la persona */
    private String apellidos;
    /** Correo electrónico */
    private String email;

    /**
     * Crea el participante a partir de un usuario del sistema, validando que
     * tenga alguno de los roles que le permiten cumplir este papel.
     *
     * @param usuario         usuario que cumplirá el rol
     * @param rolesPermitidos roles del sistema que habilitan el rol
     */
    protected Participante(Usuario usuario, Set<RoleAdmited> rolesPermitidos) {
        this(usuario == null ? null : usuario.getCusuari(),
                usuario == null ? null : usuario.getCodigo(),
                usuario == null ? null : usuario.getNombres(),
                usuario == null ? null : usuario.getApellidos(),
                usuario == null ? null : usuario.getEmail(),
                true);
        if (!tieneAlgunRol(usuario, rolesPermitidos))
            throw new ReglaNegocioException("error.dominio.participante.rol_no_permitido");
    }

    /**
     * Reconstruye el participante con datos ya persistidos.
     *
     * @param usuarioObligatorio indica si el rol exige un usuario del sistema
     */
    protected Participante(Long usuarioId, String codigo, String nombres, String apellidos, String email,
            boolean usuarioObligatorio) {
        if (usuarioObligatorio && usuarioId == null)
            throw new ReglaNegocioException("error.dominio.participante.usuario_obligatorio");
        if (CheckString.isBlank(codigo) || CheckString.isBlank(nombres) || CheckString.isBlank(apellidos)
                || CheckString.isBlank(email))
            throw new ReglaNegocioException("error.dominio.participante.datos_incompletos");
        this.usuarioId = usuarioId;
        this.codigo = codigo;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
    }

    private static boolean tieneAlgunRol(Usuario usuario, Set<RoleAdmited> rolesPermitidos) {
        if (usuario.getRole() == null)
            return false;
        return usuario.getRole().stream()
                .anyMatch(role -> rolesPermitidos.stream().anyMatch(permitido -> permitido.name()
                        .equals(role.getTrole())));
    }

    /**
     * Indica si ambos participantes son la misma persona, sin importar el rol que
     * cumplan (ej. un director que también es evaluador). Se compara por usuario
     * y, si alguno no tiene cuenta, por código.
     *
     * @param otro participante a comparar
     * @return true si corresponden a la misma persona
     */
    public boolean esMismaPersona(Participante otro) {
        if (otro == null)
            return false;
        if (this.usuarioId != null && otro.usuarioId != null)
            return this.usuarioId.equals(otro.usuarioId);
        return this.codigo.equals(otro.codigo);
    }

    /**
     * @return true si la persona tiene cuenta en el sistema
     */
    public boolean tieneUsuario() {
        return this.usuarioId != null;
    }

    public String getNombreCompleto() {
        return this.nombres + " " + this.apellidos;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getEmail() {
        return email;
    }
}
