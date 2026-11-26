package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ResumenApartado {
    private String apartado;
    private BigDecimal ingresos;
    private BigDecimal devoluciones;
    private BigDecimal neto;

    public ResumenApartado(String apartado, BigDecimal ingresos, BigDecimal devoluciones) {
        this.apartado = apartado;
        this.ingresos = ingresos;
        this.devoluciones = devoluciones;
        this.neto = ingresos.subtract(devoluciones);
    }
}