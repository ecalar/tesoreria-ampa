package com.ampa.tesoreria.dto;

import com.ampa.tesoreria.model.enums.Modalidad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InscripcionForm {

    private Long id;
    private Long actividadId;
    private Long alumnoId;
    private Modalidad modalidad;
    private Integer diasAsistidos;
    private String diasSemanaSeleccionados;
    private String observaciones;
    private Boolean permisoImagen;
}