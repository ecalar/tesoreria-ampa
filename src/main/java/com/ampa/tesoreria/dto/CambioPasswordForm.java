package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambioPasswordForm {

    private String passwordActual;
    private String passwordNueva;
    private String passwordConfirmacion;
}