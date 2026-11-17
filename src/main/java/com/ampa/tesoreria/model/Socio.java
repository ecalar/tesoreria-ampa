package com.ampa.tesoreria.model;

import com.ampa.tesoreria.model.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "socio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Socio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(length = 250)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 100)
    private String email;

    /** Permiso global de imagen (heredable por actividades) */
    @Column(nullable = false)
    private boolean permisoImagen;

    /** Nº de recibo manual (el que aparece en el PDF en papel) */
    @Column(length = 50)
    private String numeroRecibo;

    /** Cuota pagada por este socio en este curso (snapshot del precio en el momento del alta) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cuotaImporte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private LocalDate fechaAlta;

    // Relaciones

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private CursoEscolar curso;

    @OneToMany(mappedBy = "socio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Alumno> alumnos = new ArrayList<>();

    // Helpers de sincronización bidireccional

    public void addAlumno(Alumno alumno) {
        alumnos.add(alumno);
        alumno.setSocio(this);
    }

    public void removeAlumno(Alumno alumno) {
        alumnos.remove(alumno);
        alumno.setSocio(null);
    }
}