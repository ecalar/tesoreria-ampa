package com.ampa.tesoreria.dto;

import com.ampa.tesoreria.model.enums.MetodoPago;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GastoForm {

    private Long id;
    private Long categoriaId;
    private String concepto;
    private BigDecimal importeBase;
    private BigDecimal ivaPorcentaje;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    private MetodoPago metodoPago;
    private String proveedor;
    private String observaciones;

    /** Archivo nuevo de factura (null si no se cambia) */
    private MultipartFile factura;

    /** Ruta actual (para mostrar enlace al editar) */
    private String rutaFacturaActual;
    private String nombreFacturaOriginalActual;
}