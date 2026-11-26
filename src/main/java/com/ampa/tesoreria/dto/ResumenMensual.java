package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.YearMonth;

@Getter
@Setter
public class ResumenMensual {
    private YearMonth mes;
    private BigDecimal ingresos;
    private BigDecimal gastos;
    private BigDecimal saldo;

    public ResumenMensual(YearMonth mes, BigDecimal ingresos, BigDecimal gastos) {
        this.mes = mes;
        this.ingresos = ingresos;
        this.gastos = gastos;
        this.saldo = ingresos.subtract(gastos);
    }
}