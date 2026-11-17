package com.ampa.tesoreria.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grupo_foto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrupoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoFoto pedido;

    @Column(nullable = false, length = 50)
    private String curso;

    @Column(nullable = false)
    private Integer cantidad;
}