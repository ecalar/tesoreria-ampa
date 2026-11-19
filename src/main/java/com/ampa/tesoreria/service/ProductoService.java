package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.ProductoForm;
import com.ampa.tesoreria.model.PedidoProducto;
import com.ampa.tesoreria.model.Producto;
import com.ampa.tesoreria.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CursoService cursoService;

    // ------------------------------------------------------------
    // LISTADOS
    // ------------------------------------------------------------
    public List<Producto> listar(String q) {
        Long cursoId = cursoService.getCursoActivo().getId();
        if (q != null && !q.isBlank()) {
            return productoRepository.findByCursoIdAndNombreContainingIgnoreCase(cursoId, q);
        }
        return productoRepository.findByCursoId(cursoId);
    }

    public Producto findById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
    }

    // ------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------
    @Transactional
    public Producto crear(ProductoForm form) {
        Producto producto = Producto.builder()
                .curso(cursoService.getCursoActivo())
                .nombre(form.getNombre())
                .descripcion(form.getDescripcion())
                .precioUnitario(form.getPrecioUnitario())
                .activo(form.isActivo())
                .build();
        return productoRepository.save(producto);
    }

    @Transactional
    public Producto actualizar(Long id, ProductoForm form) {
        Producto p = findById(id);
        p.setNombre(form.getNombre());
        p.setDescripcion(form.getDescripcion());
        p.setPrecioUnitario(form.getPrecioUnitario());
        p.setActivo(form.isActivo());
        return productoRepository.save(p);
    }

    @Transactional
    public void eliminar(Long id) {
        Producto p = findById(id);
        long pedidos = p.getPedidos().size();
        if (pedidos > 0) {
            throw new IllegalStateException(
                    "No se puede borrar un producto con " + pedidos + " pedidos. " +
                            "Borra los pedidos primero o desactiva el producto.");
        }
        productoRepository.delete(p);
    }

    // ------------------------------------------------------------
    // UTILIDADES DE FORMULARIO
    // ------------------------------------------------------------
    public ProductoForm toForm(Producto p) {
        ProductoForm form = new ProductoForm();
        form.setId(p.getId());
        form.setNombre(p.getNombre());
        form.setDescripcion(p.getDescripcion());
        form.setPrecioUnitario(p.getPrecioUnitario());
        form.setActivo(p.isActivo());
        return form;
    }

    // ------------------------------------------------------------
    // CÁLCULOS
    // ------------------------------------------------------------
    public BigDecimal totalRecaudado(Producto p) {
        return p.getPedidos().stream()
                .map(PedidoProducto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCobrado(Producto p) {
        return p.getPedidos().stream()
                .filter(PedidoProducto::isPagado)
                .map(PedidoProducto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPendiente(Producto p) {
        return p.getPedidos().stream()
                .filter(pp -> !pp.isPagado())
                .map(PedidoProducto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long totalUnidades(Producto p) {
        return p.getPedidos().stream()
                .mapToLong(PedidoProducto::getCantidad)
                .sum();
    }

    public BigDecimal totalDevuelto(Producto p) {
        return p.getPedidos().stream()
                .filter(pp -> pp.getDevolucion() != null)
                .map(pp -> pp.getDevolucion().getImporte())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalNeto(Producto p) {
        return totalCobrado(p).subtract(totalDevuelto(p));
    }
}