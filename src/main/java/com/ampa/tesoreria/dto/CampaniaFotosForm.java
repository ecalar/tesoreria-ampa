package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CampaniaFotosForm {

    private Long id;
    private String nombre;
    private String descripcion;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFin;

    private BigDecimal precioLote;
    private BigDecimal precioGrupal;
    private BigDecimal precioIndividual;
    private BigDecimal descuentoSocioPorcentaje;
}