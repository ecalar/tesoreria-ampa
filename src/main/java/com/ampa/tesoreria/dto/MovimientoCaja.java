package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MovimientoCaja {
    private LocalDate fecha;
    private String concepto;
    private String categoria;
    private String detalle;
    private BigDecimal ingreso;
    private BigDecimal gasto;
    private BigDecimal saldoAcumulado;

    public MovimientoCaja(LocalDate fecha, String concepto, String categoria,
                          String detalle, BigDecimal ingreso, BigDecimal gasto) {
        this.fecha = fecha;
        this.concepto = concepto;
        this.categoria = categoria;
        this.detalle = detalle;
        this.ingreso = ingreso;
        this.gasto = gasto;
    }
}