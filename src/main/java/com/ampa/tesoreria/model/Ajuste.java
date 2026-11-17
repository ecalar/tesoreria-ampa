package com.ampa.tesoreria.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ajuste")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ajuste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Clave única del ajuste (ej: "cuota.socio", "descuento.socio.global") */
    @Column(nullable = false, unique = true, length = 100)
    private String clave;

    /** Valor como String (se convierte según el tipo en el servicio) */
    @Column(nullable = false, length = 500)
    private String valor;

    /** Descripción legible para la UI de Ajustes */
    @Column(length = 255)
    private String descripcion;
}