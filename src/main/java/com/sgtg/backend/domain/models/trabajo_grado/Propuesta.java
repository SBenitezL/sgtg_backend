package com.sgtg.backend.domain.models.trabajo_grado;

import java.util.Date;
import java.util.List;

import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;

public class Propuesta {
    private Long propuestaId;
    private List<Estudiante> estudiantes;
    private Director director;
    private Director codirector;
    private String titulo;
    private String modalidad;
    private Date fechaRegistro;
    private Date ultimaModificacion;
    private Estado estado;
    private Proceso proceso;
}
