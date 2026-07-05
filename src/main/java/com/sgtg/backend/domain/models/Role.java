package com.sgtg.backend.domain.models;

public class Role {
    private Long crole;
    private RoleAdmited trole;

    public Role(Long crole, RoleAdmited trole) {
        this.crole = crole;
        this.trole = trole;
    }

    public Long getCrole() {
        return crole;
    }

    public String getTrole() {
        return trole.name();
    }

    public enum RoleAdmited {
        ADMINISTRADOR,
        DOCENTE,
        ESTUDIANTE,
        DIRECTOR,
        CODIRECTOR,
        ASESOR,
        JEFE_DEP,
        COMITE,
        EVALUADOR,
        CONSEJO,
        JURADO,
        DECANATURA
    }
}
