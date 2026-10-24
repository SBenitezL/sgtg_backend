package com.sgtg.backend.domain.use_cases;

import java.util.List;

import com.sgtg.backend.application.input.catalogo.ManageCatalogoCUIntPort;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.utils.CheckString;

/** Casos de uso de los catálogos de departamentos y programas. */
public class ManageCatalogoCUImplAdapter implements ManageCatalogoCUIntPort {

    private final ManageCatalogoGatewayIntPort catalogoGateway;
    private final ExceptionFormatterIntPort exceptionFormatter;

    public ManageCatalogoCUImplAdapter(ManageCatalogoGatewayIntPort catalogoGateway,
            ExceptionFormatterIntPort exceptionFormatter) {
        this.catalogoGateway = catalogoGateway;
        this.exceptionFormatter = exceptionFormatter;
    }

    @Override
    public Departamento crearDepartamento(String nombre) {
        String limpio = validarNombre(nombre);
        if (this.catalogoGateway.existeDepartamento(limpio))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.catalogo.departamento_existente");
        return this.catalogoGateway.crearDepartamento(limpio);
    }

    @Override
    public List<Departamento> listarDepartamentos() {
        return this.catalogoGateway.listarDepartamentos();
    }

    @Override
    public Programa crearPrograma(String nombre, Long departamentoId) {
        String limpio = validarNombre(nombre);
        if (departamentoId == null)
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.catalogo.programa_sin_departamento");
        Departamento departamento = this.catalogoGateway.buscarDepartamento(departamentoId);
        if (departamento == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.catalogo.departamento_no_encontrado");
        if (this.catalogoGateway.existePrograma(limpio))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.catalogo.programa_existente");
        return this.catalogoGateway.crearPrograma(limpio, departamento);
    }

    @Override
    public List<Programa> listarProgramas() {
        return this.catalogoGateway.listarProgramas();
    }

    private String validarNombre(String nombre) {
        if (CheckString.isBlank(nombre))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.catalogo.nombre_obligatorio");
        return nombre.trim();
    }
}
