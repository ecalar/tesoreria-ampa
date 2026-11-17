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

    @Column(length = 50)
    private String curso;

    private LocalDate fechaNacimiento;

    @Column(length = 250)
    private String direccion;

    @Column(length = 200)
    private String centroEducativo;

    @Column(length = 500)
    private String alergias;

    @Column(length = 1000)
    private String observacionesMedicas;

    /** Autorizacion al centro para trasladar al alumno a un centro médico en caso de urgencia */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private boolean autorizacionDesplazamientoMedico = false;

    // --- Datos del responsable legal (solo si no es socio) ---

    @Column(length = 150)
    private String nombreResponsable;

    @Column(length = 20)
    private String dniResponsable;

    @Column(length = 20)
    private String telefonoResponsable;

    @Column(length = 100)
    private String emailResponsable;

    @Column(length = 50)
    private String parentescoResponsable;

    // Relaciones

    /** Puede ser null si el alumno no es socio del AMPA */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_id")
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

    @Transient
    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }

    @Transient
    public boolean isEsSocio() {
        return socio != null;
    }

    /** Nombre del responsable legal: si tiene socio, sale del socio; si no, del campo manual */
    @Transient
    public String getNombreResponsableMostrar() {
        if (socio != null) {
            return socio.getNombre() + " " + socio.getApellidos();
        }
        return nombreResponsable;
    }

    /** Email del responsable: socio o manual */
    @Transient
    public String getEmailResponsableMostrar() {
        if (socio != null) return socio.getEmail();
        return emailResponsable;
    }

    /** Telefono del responsable: socio o manual */
    @Transient
    public String getTelefonoResponsableMostrar() {
        if (socio != null) return socio.getTelefono();
        return telefonoResponsable;
    }
}