package com.ampa.tesoreria.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alumno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String apellidos;

    /** Curso actual del alumno en el colegio (ej: "3ºA", "5 años B", "PLURI") */
    @Column(length = 50)
    private String curso;

    private LocalDate fechaNacimiento;

    @Column(length = 500)
    private String alergias;

    @Column(length = 1000)
    private String observacionesMedicas;

    // Relaciones

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("orden ASC")
    private List<ContactoEmergencia> contactosEmergencia = new ArrayList<>();

    // Helpers

    public void addContactoEmergencia(ContactoEmergencia contacto) {
        contactosEmergencia.add(contacto);
        contacto.setAlumno(this);
    }

    public void removeContactoEmergencia(ContactoEmergencia contacto) {
        contactosEmergencia.remove(contacto);
        contacto.setAlumno(null);
    }

    /** Nombre completo para mostrar en listados */
    @Transient
    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}