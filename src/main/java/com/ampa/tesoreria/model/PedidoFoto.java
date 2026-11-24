package com.ampa.tesoreria.model;

import com.ampa.tesoreria.model.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "pedido_foto",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campania_id", "alumno_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campania_id", nullable = false)
    private CampaniaFotos campania;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    /** Snapshot del curso del alumno en el momento del pedido */
    @Column(length = 50)
    private String cursoAlumno;

    // Cantidades

    @Column(nullable = false)
    @Builder.Default
    private Integer numLotes = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer numIndividuales = 0;

    // Snapshot de precios aplicados

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioLoteAplicado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioGrupalAplicado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioIndividualAplicado;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;

    // Importes calculados

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // Pago

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(nullable = false)
    @Builder.Default
    private boolean pagado = false;

    private LocalDate fechaPago;

    @Column(nullable = false)
    private LocalDate fechaPedido;

    @Column(length = 1000)
    private String observaciones;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DevolucionPedidoFoto devolucion;

    // Relaciones

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GrupoFoto> grupos = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<HermanoFoto> hermanos = new HashSet<>();

    // Helpers

    public void addGrupo(GrupoFoto grupo) {
        grupos.add(grupo);
        grupo.setPedido(this);
    }

    public void removeGrupo(GrupoFoto grupo) {
        grupos.remove(grupo);
        grupo.setPedido(null);
    }

    public void addHermano(HermanoFoto hermano) {
        hermanos.add(hermano);
        hermano.setPedido(this);
    }

    public void removeHermano(HermanoFoto hermano) {
        hermanos.remove(hermano);
        hermano.setPedido(null);
    }

    // En PedidoFoto.java
    @Transient
    public int getTotalGrupales() {
        return grupos == null ? 0
                : grupos.stream().mapToInt(GrupoFoto::getCantidad).sum();
    }

    @Transient
    public boolean getTieneHermanos() {
        return hermanos != null && !hermanos.isEmpty();
    }

    public void setDevolucion(DevolucionPedidoFoto devolucion) {
        this.devolucion = devolucion;
        if (devolucion != null) {
            devolucion.setPedido(this);
        }
    }
}