package com.sgtg.backend.application.output;

import java.util.List;

import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Programa;

/** Puerto de salida para la persistencia de departamentos y programas. */
public interface ManageCatalogoGatewayIntPort {

    public Departamento crearDepartamento(String nombre);

    public boolean existeDepartamento(String nombre);

    /** @return el departamento, o null si no existe */
    public Departamento buscarDepartamento(Long id);

    public List<Departamento> listarDepartamentos();

    public Programa crearPrograma(String nombre);

    public boolean existePrograma(String nombre);

    /** @return el programa, o null si no existe */
    public Programa buscarPrograma(Long id);

    public List<Programa> listarProgramas();
}
