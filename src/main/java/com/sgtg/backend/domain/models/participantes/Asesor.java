package com.sgtg.backend.domain.models.participantes;

import java.util.Set;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Profesional que orienta el trabajo de grado. Puede pertenecer o no a la
 * universidad (Art. 10); en práctica profesional es obligatorio y pertenece a
 * la organización donde se realiza (Art. 32). Es el único participante que
 * puede no tener cuenta en el sistema.
 */
public class Asesor extends Participante {
    /** Roles del sistema que permiten ser asesor de un trabajo de grado */
    private static final Set<RoleAdmited> ROLES_PERMITIDOS = Set.of(RoleAdmited.ASESOR, RoleAdmited.DOCENTE);

    /** Organización a la que pertenece, obligatoria si es externo */
    private String organizacion;
    /** Indica si el asesor es externo a la universidad */
    private boolean externo;

    /** Crea el asesor a partir de un usuario con rol ASESOR o DOCENTE. */
    public Asesor(Usuario usuario, String organizacion, boolean externo) {
        super(usuario, ROLES_PERMITIDOS);
        this.organizacion = validarOrganizacion(organizacion, externo);
        this.externo = externo;
    }

    /**
     * Crea o reconstruye un asesor con sus datos. El usuario es opcional: un
     * asesor externo puede no tener cuenta en el sistema; en ese caso el código
     * puede ser su documento de identidad.
     */
    public Asesor(Long usuarioId, String codigo, NombrePersona nombre, String email, String organizacion,
            boolean externo) {
        super(usuarioId, codigo, nombre, email, false);
        this.organizacion = validarOrganizacion(organizacion, externo);
        this.externo = externo;
    }

    private static String validarOrganizacion(String organizacion, boolean externo) {
        if (externo && CheckString.isBlank(organizacion))
            throw new ReglaNegocioException("error.dominio.asesor.organizacion_obligatoria");
        return organizacion;
    }

    public String getOrganizacion() {
        return organizacion;
    }

    public boolean isExterno() {
        return externo;
    }
}
