package com.ampa.tesoreria.model;

import com.ampa.tesoreria.model.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gasto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private CursoEscolar curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaGasto categoria;

    @Column(nullable = false, length = 200)
    private String concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importeBase;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal ivaPorcentaje = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importeTotal;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(length = 150)
    private String proveedor;

    @Column(length = 500)
    private String rutaFactura;

    @Column(length = 255)
    private String nombreFacturaOriginal;

    @Column(length = 1000)
    private String observaciones;
}