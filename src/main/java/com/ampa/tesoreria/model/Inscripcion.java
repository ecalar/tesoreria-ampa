package com.ampa.tesoreria.model;

import com.ampa.tesoreria.model.enums.EstadoFianza;
import com.ampa.tesoreria.model.enums.EstadoInscripcion;
import com.ampa.tesoreria.model.enums.MetodoPago;
import com.ampa.tesoreria.model.enums.Modalidad;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inscripcion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"actividad_id", "alumno_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidad modalidad;

    /** Solo si modalidad = POR_DIAS: nº de días al mes */
    private Integer diasAsistidos;

    /** Solo si modalidad = POR_DIAS: días concretos elegidos, ej "L,M,X" */
    @Column(length = 50)
    private String diasSemanaSeleccionados;

    /** Importe mensual calculado al inscribir (snapshot). Ya incluye descuento socio. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importeMensualCalculado;

    /** Permiso de imagen específico de esta inscripción. null = hereda del socio */
    private Boolean permisoImagen;

    @Column(nullable = false)
    private LocalDate fechaInscripcion;

    @Column(length = 1000)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoInscripcion estado = EstadoInscripcion.ACTIVA;

    // Fianza

    @Column(precision = 10, scale = 2)
    private BigDecimal fianzaImporte;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoFianza fianzaEstado;

    private LocalDate fianzaFechaPago;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MetodoPago fianzaMetodoPago;

    @Column(length = 500)
    private String fianzaObservaciones;

    @OneToOne(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DevolucionInscripcion devolucionInscripcion;

    // Relaciones

    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("mes ASC")
    @Builder.Default
    private List<PagoMensual> pagosMensuales = new ArrayList<>();

    @OneToOne(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DevolucionFianza devolucionFianza;

    // Helpers

    public void addPagoMensual(PagoMensual pago) {
        pagosMensuales.add(pago);
        pago.setInscripcion(this);
    }

    public void removePagoMensual(PagoMensual pago) {
        pagosMensuales.remove(pago);
        pago.setInscripcion(null);
    }

    public void setDevolucionFianza(DevolucionFianza devolucion) {
        this.devolucionFianza = devolucion;
        if (devolucion != null) {
            devolucion.setInscripcion(this);
        }
    }
    public void setDevolucionInscripcion(DevolucionInscripcion devolucion) {
        this.devolucionInscripcion = devolucion;
        if (devolucion != null) {
            devolucion.setInscripcion(this);
        }
    }

    @Transient
    public boolean tieneFianza() {
        return fianzaImporte != null && fianzaImporte.signum() > 0;
    }
}