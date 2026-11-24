package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlumnoSimpleDTO {
    private Long id;
    private String nombreCompleto;
    private String curso;
    private boolean esSocio;
}