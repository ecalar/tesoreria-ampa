package com.ampa.tesoreria.dto;

import com.ampa.tesoreria.model.enums.MetodoPago;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoProductoForm {

    private Long id;
    private Long productoId;
    private Long alumnoId;
    private Integer cantidad;
    private String talla;
    private MetodoPago metodoPago;
    private String observaciones;
}