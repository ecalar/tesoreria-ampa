package com.ampa.tesoreria.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hermano_foto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HermanoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoFoto pedido;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 50)
    private String curso;
}