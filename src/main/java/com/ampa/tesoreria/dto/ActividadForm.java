package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ActividadForm {

    private Long id;
    private String nombre;
    private String descripcion;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFin;

    /** Días seleccionados como checkboxes: L, M, X, J, V */
    private List<String> diasSeleccionados = new ArrayList<>();

    private String horario;

    private BigDecimal precioMesCompleto;
    private BigDecimal precioMedioMes;
    private BigDecimal descuentoSocioPorcentaje;

    private boolean tieneFianza;
    private BigDecimal importeFianza;
}