package com.ampa.tesoreria.model;

import com.ampa.tesoreria.model.enums.EstadoCurso;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "curso_escolar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ej: "2024-2025" */
    @Column(nullable = false, unique = true, length = 20)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCurso estado;

    // Relaciones (cargadas de forma perezosa, se rellenan desde las otras entidades)
    @OneToMany(mappedBy = "curso", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Socio> socios = new ArrayList<>();

    @OneToMany(mappedBy = "curso", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Actividad> actividades = new ArrayList<>();

    @OneToMany(mappedBy = "curso", fetch = FetchType.LAZY)
    @Builder.Default
    private List<CampaniaFotos> campaniasFotos = new ArrayList<>();

    @OneToMany(mappedBy = "curso", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Gasto> gastos = new ArrayList<>();
}