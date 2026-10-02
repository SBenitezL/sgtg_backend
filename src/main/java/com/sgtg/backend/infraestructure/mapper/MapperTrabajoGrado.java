package com.sgtg.backend.infraestructure.mapper;

import java.util.List;

import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.trabajo_grado.CambioEtapa;
import com.sgtg.backend.domain.models.trabajo_grado.DatosPropuesta;
import com.sgtg.backend.domain.models.trabajo_grado.DatosTrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.Documento;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Propuesta;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.AsesorEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.CambioEtapaEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.DecisionEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.DirectorTrabajoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.DirectorTrabajoEmbeddable.RolDireccion;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.DocumentoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.EstudianteTrabajoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.TrabajoGradoEntity;

/**
 * Conversión entre el agregado {@link TrabajoGrado} y su entidad. La lectura
 * usa {@link TrabajoGrado#reconstruir}, que no vuelve a aplicar las reglas de
 * creación.
 */
public abstract class MapperTrabajoGrado {

    // ----- Dominio → entidad -----

    public static TrabajoGradoEntity toEntity(TrabajoGrado trabajo) {
        TrabajoGradoEntity entity = new TrabajoGradoEntity();
        entity.setId(trabajo.getId());
        entity.setModalidad(trabajo.getModalidad());
        entity.setTitulo(trabajo.getTitulo());
        entity.setEtapa(trabajo.getEtapa());
        entity.setMotivoCierre(trabajo.getMotivoCierre());
        entity.getObjetivos().addAll(trabajo.getObjetivos());
        trabajo.getEstudiantes().forEach(e -> entity.getEstudiantes().add(toEmbeddable(e)));
        entity.getDireccion().add(toEmbeddable(trabajo.getDirector(), RolDireccion.DIRECTOR));
        if (trabajo.getCodirector() != null)
            entity.getDireccion().add(toEmbeddable(trabajo.getCodirector(), RolDireccion.CODIRECTOR));
        entity.setAsesor(toEmbeddable(trabajo.getAsesor()));
        if (trabajo instanceof PracticaProfesional practica)
            entity.setCartaAceptacion(toEmbeddable(practica.getCartaAceptacion()));

        Propuesta propuesta = trabajo.getPropuesta();
        entity.setPropuestaEstado(propuesta.getEstado());
        entity.setPropuestaFechaLimiteConcepto(propuesta.getFechaLimiteConcepto());
        propuesta.getVersiones().forEach(v -> entity.getPropuestaVersiones().add(toEmbeddable(v)));
        propuesta.getDecisiones().forEach(d -> entity.getPropuestaDecisiones().add(toEmbeddable(d)));

        trabajo.getHistorial().forEach(c -> entity.getHistorial().add(toEmbeddable(c)));
        return entity;
    }

    private static EstudianteTrabajoEmbeddable toEmbeddable(Estudiante e) {
        NombrePersona n = e.getNombre();
        return new EstudianteTrabajoEmbeddable(e.getUsuarioId(), e.getCodigo(), n.primerNombre(), n.segundoNombre(),
                n.primerApellido(), n.segundoApellido(), e.getEmail(), e.getPrograma().id(), e.getPrograma().nombre());
    }

    private static DirectorTrabajoEmbeddable toEmbeddable(Director d, RolDireccion rol) {
        NombrePersona n = d.getNombre();
        return new DirectorTrabajoEmbeddable(rol, d.getUsuarioId(), d.getCodigo(), n.primerNombre(),
                n.segundoNombre(), n.primerApellido(), n.segundoApellido(), d.getEmail(), d.getTipoVinculacion(),
                d.getDepartamento().id(), d.getDepartamento().nombre());
    }

    private static AsesorEmbeddable toEmbeddable(Asesor a) {
        if (a == null)
            return null;
        NombrePersona n = a.getNombre();
        return new AsesorEmbeddable(a.getUsuarioId(), a.getCodigo(), n.primerNombre(), n.segundoNombre(),
                n.primerApellido(), n.segundoApellido(), a.getEmail(), a.getOrganizacion(), a.isExterno());
    }

    private static DocumentoEmbeddable toEmbeddable(Documento d) {
        if (d == null)
            return null;
        return new DocumentoEmbeddable(d.tipo(), d.url(), d.version(), d.fechaCarga());
    }

    private static DecisionEmbeddable toEmbeddable(Decision d) {
        return new DecisionEmbeddable(d.organo(), d.resultado(), d.acta(), d.rutaArchivo(), d.observaciones(),
                d.fecha());
    }

    private static CambioEtapaEmbeddable toEmbeddable(CambioEtapa c) {
        return new CambioEtapaEmbeddable(c.origen(), c.destino(), c.fecha(), c.responsable(), c.observacion());
    }

    // ----- Entidad → dominio -----

    public static TrabajoGrado toDomain(TrabajoGradoEntity entity) {
        if (entity == null)
            return null;
        Director director = buscarDireccion(entity.getDireccion(), RolDireccion.DIRECTOR);
        Director codirector = buscarDireccion(entity.getDireccion(), RolDireccion.CODIRECTOR);
        DatosPropuesta propuesta = new DatosPropuesta(
                entity.getPropuestaVersiones().stream().map(MapperTrabajoGrado::toDomain).toList(),
                entity.getPropuestaDecisiones().stream().map(MapperTrabajoGrado::toDomain).toList(),
                entity.getPropuestaEstado(), entity.getPropuestaFechaLimiteConcepto());
        return TrabajoGrado.reconstruir(new DatosTrabajoGrado(
                entity.getId(), entity.getModalidad(), entity.getTitulo(), List.copyOf(entity.getObjetivos()),
                entity.getEstudiantes().stream().map(MapperTrabajoGrado::toDomain).toList(),
                director, codirector, toDomain(entity.getAsesor()), toDomain(entity.getCartaAceptacion()),
                entity.getEtapa(), propuesta,
                entity.getHistorial().stream().map(MapperTrabajoGrado::toDomain).toList(),
                entity.getMotivoCierre()));
    }

    private static Director buscarDireccion(List<DirectorTrabajoEmbeddable> direccion, RolDireccion rol) {
        return direccion.stream()
                .filter(d -> d.getRol() == rol)
                .findFirst()
                .map(d -> new Director(d.getUsuarioId(), d.getCodigo(),
                        new NombrePersona(d.getPrimerNombre(), d.getSegundoNombre(), d.getPrimerApellido(),
                                d.getSegundoApellido()),
                        d.getEmail(), d.getTipoVinculacion(),
                        new Departamento(d.getDepartamentoId(), d.getDepartamentoNombre())))
                .orElse(null);
    }

    private static Estudiante toDomain(EstudianteTrabajoEmbeddable e) {
        return new Estudiante(e.getUsuarioId(), e.getCodigo(),
                new NombrePersona(e.getPrimerNombre(), e.getSegundoNombre(), e.getPrimerApellido(),
                        e.getSegundoApellido()),
                e.getEmail(), new Programa(e.getProgramaId(), e.getProgramaNombre()));
    }

    private static Asesor toDomain(AsesorEmbeddable a) {
        if (a == null || a.getCodigo() == null)
            return null;
        return new Asesor(a.getUsuarioId(), a.getCodigo(),
                new NombrePersona(a.getPrimerNombre(), a.getSegundoNombre(), a.getPrimerApellido(),
                        a.getSegundoApellido()),
                a.getEmail(), a.getOrganizacion(), Boolean.TRUE.equals(a.getExterno()));
    }

    private static Documento toDomain(DocumentoEmbeddable d) {
        if (d == null || d.getTipo() == null)
            return null;
        return new Documento(d.getTipo(), d.getUrl(), d.getVersion(), d.getFechaCarga());
    }

    private static Decision toDomain(DecisionEmbeddable d) {
        return new Decision(d.getOrgano(), d.getResultado(), d.getActa(), d.getRutaArchivo(), d.getObservaciones(),
                d.getFecha());
    }

    private static CambioEtapa toDomain(CambioEtapaEmbeddable c) {
        return new CambioEtapa(c.getOrigen(), c.getDestino(), c.getFecha(), c.getResponsable(), c.getObservacion());
    }
}
