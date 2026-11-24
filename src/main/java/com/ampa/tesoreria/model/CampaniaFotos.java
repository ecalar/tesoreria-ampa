package com.ampa.tesoreria.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campania_fotos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaniaFotos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private CursoEscolar curso;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioLote;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioGrupal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioIndividual;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descuentoSocioPorcentaje = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean cerrada = false;

    @OneToMany(mappedBy = "campania", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PedidoFoto> pedidos = new ArrayList<>();

    public void addPedido(PedidoFoto pedido) {
        pedidos.add(pedido);
        pedido.setCampania(this);
    }

    public void removePedido(PedidoFoto pedido) {
        pedidos.remove(pedido);
        pedido.setCampania(null);
    }
}