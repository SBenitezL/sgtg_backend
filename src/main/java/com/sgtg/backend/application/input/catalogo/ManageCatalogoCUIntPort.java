package com.sgtg.backend.application.input.catalogo;

import java.util.List;

import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Programa;

/** Puerto de entrada para administrar los catálogos de departamentos y programas. */
public interface ManageCatalogoCUIntPort {

    /**
     * Registra un departamento de la Facultad.
     *
     * @param nombre nombre del departamento, único
     * @return el departamento creado con su identificador
     */
    public Departamento crearDepartamento(String nombre);

    /** @return los departamentos registrados, ordenados por nombre */
    public List<Departamento> listarDepartamentos();

    /**
     * Registra un programa académico de pregrado, adscrito a un departamento:
     * su Comité de Programa lo forman docentes de ese departamento.
     *
     * @param nombre         nombre del programa, único
     * @param departamentoId departamento al que está adscrito
     * @return el programa creado con su identificador y su departamento
     */
    public Programa crearPrograma(String nombre, Long departamentoId);

    /** @return los programas registrados, ordenados por nombre */
    public List<Programa> listarProgramas();
}
