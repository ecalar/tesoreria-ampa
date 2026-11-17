package com.ampa.tesoreria.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contacto_emergencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactoEmergencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 50)
    private String parentesco;

    /** Orden de llamada: 1 = primero, 2 = segundo... (mínimo 4 huecos en el formulario) */
    @Column(nullable = false)
    private Integer orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;
}