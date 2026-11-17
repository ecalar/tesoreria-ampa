package com.ampa.tesoreria.dto;

import com.ampa.tesoreria.model.enums.MetodoPago;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocioForm {

    private Long id;
    private String nombre;
    private String apellidos;
    private String dni;
    private String direccion;
    private String telefono;
    private String email;
    private boolean permisoImagen;
    private String numeroRecibo;
    private MetodoPago metodoPago;
}