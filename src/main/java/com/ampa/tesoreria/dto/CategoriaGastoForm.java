package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaGastoForm {

    private Long id;
    private String nombre;
    private String descripcion;
    private boolean activa;
}