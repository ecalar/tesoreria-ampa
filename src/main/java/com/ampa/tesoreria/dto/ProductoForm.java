package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductoForm {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precioUnitario;
    private boolean activo;
}