package com.ampa.tesoreria.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AlumnoForm {

    private Long id;
    private Long socioId;
    private String nombre;
    private String apellidos;
    private String curso;
    private LocalDate fechaNacimiento;
    private String direccion;
    private String centroEducativo;
    private String alergias;
    private String observacionesMedicas;
    private boolean autorizacionDesplazamientoMedico;

    // Solo si no es socio
    private String nombreResponsable;
    private String dniResponsable;
    private String telefonoResponsable;
    private String emailResponsable;
    private String parentescoResponsable;

    private List<ContactoForm> contactos = new ArrayList<>();

    @Getter
    @Setter
    public static class ContactoForm {
        private Long id;
        private String nombre;
        private String telefono;
        private String parentesco;
        private Integer orden;
    }
}