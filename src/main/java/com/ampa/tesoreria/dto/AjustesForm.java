package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AjustesForm {

    private BigDecimal cuotaSocio;
    private BigDecimal descuentoSocioGlobal;

    private String nombreAmpa;
    private String cifAmpa;
    private String direccionAmpa;
    private String telefonoAmpa;
    private String emailAmpa;
    private String cuentaAmpa;

    private boolean ivaActivado;
    private BigDecimal ivaPorcentaje;

    private int sesionTimeoutMinutos;
}