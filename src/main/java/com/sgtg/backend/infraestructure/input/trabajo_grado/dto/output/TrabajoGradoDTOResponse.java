package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.output;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.models.trabajo_grado.CambioEtapa;
import com.sgtg.backend.domain.models.trabajo_grado.Documento;
import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;
import com.sgtg.backend.domain.models.trabajo_grado.MotivoCierre;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.Propuesta;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.infraestructure.input.catalogo.dto.output.CatalogoDTOResponse;

/** Vista completa de un trabajo de grado. */
public record TrabajoGradoDTOResponse(Long id, Modalidad modalidad, String titulo, List<String> objetivos,
        Proceso etapa, MotivoCierre motivoCierre, List<EstudianteDTO> estudiantes, DirectorDTO director,
        DirectorDTO codirector, AsesorDTO asesor, DocumentoDTO cartaAceptacion, PropuestaDTO propuesta,
        List<CambioEtapaDTO> historial) {

    public record EstudianteDTO(Long usuarioId, String codigo, String nombreCompleto, String email,
            CatalogoDTOResponse programa) {
    }

    public record DirectorDTO(Long usuarioId, String codigo, String nombreCompleto, String email,
            TipoVinculacion tipoVinculacion, CatalogoDTOResponse departamento) {
    }

    public record AsesorDTO(Long usuarioId, String codigo, String nombreCompleto, String email, String organizacion,
            boolean externo) {
    }

    /** @param formato código del formato del reglamento (ej. TI-A), null si no aplica */
    public record DocumentoDTO(String tipo, String formato, String url, int version, LocalDate fechaCarga) {
    }

    public record DecisionDTO(Organo organo, ResultadoConcepto resultado, String acta, String rutaArchivo,
            String observaciones,
            LocalDate fecha) {
    }

    public record PropuestaDTO(Estado estado, LocalDate fechaLimiteConcepto, List<DocumentoDTO> versiones,
            List<DecisionDTO> decisiones) {
    }

    public record CambioEtapaDTO(Proceso origen, Proceso destino, LocalDateTime fecha, String responsable,
            String observacion) {
    }

    public static TrabajoGradoDTOResponse desde(TrabajoGrado trabajo) {
        Modalidad modalidad = trabajo.getModalidad();
        Propuesta propuesta = trabajo.getPropuesta();
        DocumentoDTO carta = trabajo instanceof PracticaProfesional practica
                ? documento(practica.getCartaAceptacion(), modalidad)
                : null;
        return new TrabajoGradoDTOResponse(trabajo.getId(), modalidad, trabajo.getTitulo(), trabajo.getObjetivos(),
                trabajo.getEtapa(), trabajo.getMotivoCierre(),
                trabajo.getEstudiantes().stream().map(TrabajoGradoDTOResponse::estudiante).toList(),
                director(trabajo.getDirector()), director(trabajo.getCodirector()), asesor(trabajo.getAsesor()), carta,
                new PropuestaDTO(propuesta.getEstado(), propuesta.getFechaLimiteConcepto(),
                        propuesta.getVersiones().stream().map(v -> documento(v, modalidad)).toList(),
                        propuesta.getDecisiones().stream().map(TrabajoGradoDTOResponse::decision).toList()),
                trabajo.getHistorial().stream().map(TrabajoGradoDTOResponse::cambio).toList());
    }

    private static EstudianteDTO estudiante(Estudiante e) {
        return new EstudianteDTO(e.getUsuarioId(), e.getCodigo(), e.getNombreCompleto(), e.getEmail(),
                CatalogoDTOResponse.desde(e.getPrograma()));
    }

    private static DirectorDTO director(Director d) {
        if (d == null)
            return null;
        return new DirectorDTO(d.getUsuarioId(), d.getCodigo(), d.getNombreCompleto(), d.getEmail(),
                d.getTipoVinculacion(), CatalogoDTOResponse.desde(d.getDepartamento()));
    }

    private static AsesorDTO asesor(Asesor a) {
        if (a == null)
            return null;
        return new AsesorDTO(a.getUsuarioId(), a.getCodigo(), a.getNombreCompleto(), a.getEmail(),
                a.getOrganizacion(), a.isExterno());
    }

    private static DocumentoDTO documento(Documento d, Modalidad modalidad) {
        if (d == null)
            return null;
        return new DocumentoDTO(d.tipo().name(), d.tipo().formato(modalidad).orElse(null), d.url(), d.version(),
                d.fechaCarga());
    }

    private static DecisionDTO decision(Decision d) {
        return new DecisionDTO(d.organo(), d.resultado(), d.acta(), d.rutaArchivo(), d.observaciones(),
                d.fecha());
    }

    private static CambioEtapaDTO cambio(CambioEtapa c) {
        return new CambioEtapaDTO(c.origen(), c.destino(), c.fecha(), c.responsable(), c.observacion());
    }
}
