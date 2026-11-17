package com.ampa.tesoreria.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Contraseña encriptada con BCrypt */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false)
    private boolean activo = true;
}