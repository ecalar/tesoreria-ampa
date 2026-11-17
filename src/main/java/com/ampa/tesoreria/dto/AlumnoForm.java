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
    private String alergias;
    private String observacionesMedicas;

    /** Hasta 4 contactos de emergencia (los nulos se ignoran al guardar) */
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