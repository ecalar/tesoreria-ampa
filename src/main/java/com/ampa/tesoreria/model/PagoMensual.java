package com.ampa.tesoreria.model;

import com.ampa.tesoreria.model.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(name = "pago_mensual",
        uniqueConstraints = @UniqueConstraint(columnNames = {"inscripcion_id", "mes"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    /** Año-mes del pago (ej: 2024-10, 2024-11...) */
    @Column(nullable = false)
    private YearMonth mes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false)
    @Builder.Default
    private boolean pagado = false;

    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MetodoPago metodoPago;

    /** true si este mes lo cubre la fianza (ej: mayo en extraescolares) */
    @Column(nullable = false)
    @Builder.Default
    private boolean cubiertoPorFianza = false;

    @Column(length = 500)
    private String observaciones;
}