package com.ampa.tesoreria.model;

import com.ampa.tesoreria.model.enums.TipoActividad;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "actividad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private CursoEscolar curso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoActividad tipo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    /** Días de la semana en que se imparte, ej: "L,M,X" o "L-V" */
    @Column(length = 50)
    private String diasSemana;

    /** Horario, ej: "16:00-17:30" */
    @Column(length = 50)
    private String horario;

    // Precios

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMesCompleto;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioMedioMes;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioPorDia;

    /** % de descuento para hijos de socios (0-100) */
    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descuentoSocioPorcentaje = BigDecimal.ZERO;

    // Fianza (solo extraescolares)

    @Column(nullable = false)
    @Builder.Default
    private boolean tieneFianza = false;

    @Column(precision = 10, scale = 2)
    private BigDecimal importeFianza;

    // Relaciones

    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Inscripcion> inscripciones = new ArrayList<>();

    // Helpers

    public void addInscripcion(Inscripcion inscripcion) {
        inscripciones.add(inscripcion);
        inscripcion.setActividad(this);
    }

    public void removeInscripcion(Inscripcion inscripcion) {
        inscripciones.remove(inscripcion);
        inscripcion.setActividad(null);
    }

    @Transient
    public boolean esExtraescolar() {
        return tipo == TipoActividad.EXTRAESCOLAR;
    }
}