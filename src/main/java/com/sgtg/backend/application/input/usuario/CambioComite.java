package com.sgtg.backend.application.input.usuario;

import java.util.List;

import com.sgtg.backend.domain.models.participantes.Departamento;

/**
 * Resultado de cambiar el Comité de un departamento.
 *
 * @param comite       quien quedó como Comité, con sus perfiles
 * @param departamento departamento del Comité
 * @param anteriores   quienes dejaron de ser el Comité (normalmente uno)
 */
public record CambioComite(FichaUsuario comite, Departamento departamento, List<Long> anteriores) {
}
