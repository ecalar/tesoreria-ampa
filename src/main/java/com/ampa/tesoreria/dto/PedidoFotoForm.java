package com.ampa.tesoreria.dto;

import com.ampa.tesoreria.model.enums.MetodoPago;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PedidoFotoForm {

    private Long id;
    private Long campaniaId;
    private Long alumnoId;

    private Integer numLotes;
    private Integer numIndividuales;

    private List<GrupoForm> grupos = new ArrayList<>();
    private List<HermanoForm> hermanos = new ArrayList<>();

    private MetodoPago metodoPago;
    private String observaciones;
    private String hermanosExternos;

    @Getter
    @Setter
    public static class GrupoForm {
        private String curso;
        private Integer cantidad;
    }

    @Getter
    @Setter
    public static class HermanoForm {
        private String nombre;
        private String curso;
    }
}