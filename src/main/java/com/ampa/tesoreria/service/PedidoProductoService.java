package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.PedidoProductoForm;
import com.ampa.tesoreria.model.Alumno;
import com.ampa.tesoreria.model.DevolucionPedidoProducto;
import com.ampa.tesoreria.model.PedidoProducto;
import com.ampa.tesoreria.model.Producto;
import com.ampa.tesoreria.model.enums.MetodoPago;
import com.ampa.tesoreria.repository.PedidoProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoProductoService {

    private final PedidoProductoRepository pedidoRepository;
    private final ProductoService productoService;
    private final AlumnoService alumnoService;

    // ------------------------------------------------------------
    // LISTADOS
    // ------------------------------------------------------------
    public List<PedidoProducto> listarPorProducto(Long productoId, String q) {
        List<PedidoProducto> pedidos = pedidoRepository.findByProductoId(productoId);
        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            pedidos = pedidos.stream()
                    .filter(p -> p.getAlumno().getNombreCompleto().toLowerCase().contains(query)
                            || (p.getAlumno().getCurso() != null
                            && p.getAlumno().getCurso().toLowerCase().contains(query)))
                    .toList();
        }
        return pedidos;
    }

    public PedidoProducto findById(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
    }

    // ------------------------------------------------------------
    // CREAR / ACTUALIZAR
    // ------------------------------------------------------------
    @Transactional
    public PedidoProducto crear(PedidoProductoForm form) {
        Producto producto = productoService.findById(form.getProductoId());
        Alumno alumno = alumnoService.findById(form.getAlumnoId());

        if (form.getCantidad() == null || form.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }

        BigDecimal precio = producto.getPrecioUnitario();
        BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(form.getCantidad()));

        PedidoProducto pedido = PedidoProducto.builder()
                .producto(producto)
                .alumno(alumno)
                .precioUnitarioAplicado(precio)
                .cantidad(form.getCantidad())
                .subtotal(subtotal)
                .talla(form.getTalla())
                .metodoPago(form.getMetodoPago())
                .pagado(false)
                .fechaPedido(LocalDate.now())
                .observaciones(form.getObservaciones())
                .build();

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public PedidoProducto actualizar(Long id, PedidoProductoForm form) {
        PedidoProducto pedido = findById(id);

        // Si cambia la cantidad, recalcular subtotal
        if (!pedido.getCantidad().equals(form.getCantidad())) {
            pedido.setCantidad(form.getCantidad());
            pedido.setSubtotal(pedido.getPrecioUnitarioAplicado()
                    .multiply(BigDecimal.valueOf(form.getCantidad())));
        }

        pedido.setTalla(form.getTalla());
        pedido.setMetodoPago(form.getMetodoPago());
        pedido.setObservaciones(form.getObservaciones());

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public void eliminar(Long id) {
        pedidoRepository.deleteById(id);
    }

    // ------------------------------------------------------------
    // MARCAR PAGADO / PENDIENTE
    // ------------------------------------------------------------
    @Transactional
    public void marcarPagado(Long id, LocalDate fecha) {
        PedidoProducto p = findById(id);
        p.setPagado(true);
        p.setFechaPago(fecha != null ? fecha : LocalDate.now());
        pedidoRepository.save(p);
    }

    @Transactional
    public void desmarcarPagado(Long id) {
        PedidoProducto p = findById(id);
        p.setPagado(false);
        p.setFechaPago(null);
        pedidoRepository.save(p);
    }

    // ------------------------------------------------------------
// DEVOLUCION
// ------------------------------------------------------------
    @Transactional
    public void registrarDevolucion(Long pedidoId, LocalDate fecha, BigDecimal importe,
                                    MetodoPago metodo, String observaciones) {
        PedidoProducto p = findById(pedidoId);
        if (!p.isPagado()) {
            throw new IllegalStateException(
                    "Solo se puede devolver un pedido que ya este pagado");
        }
        if (p.getDevolucion() != null) {
            throw new IllegalStateException("Este pedido ya tiene una devolucion registrada");
        }
        if (importe == null || importe.signum() <= 0) {
            throw new IllegalArgumentException("El importe a devolver debe ser mayor que 0");
        }
        if (importe.compareTo(p.getSubtotal()) > 0) {
            throw new IllegalArgumentException(
                    "El importe a devolver no puede superar el subtotal (" + p.getSubtotal() + " EUR)");
        }

        DevolucionPedidoProducto dev = DevolucionPedidoProducto.builder()
                .fecha(fecha != null ? fecha : LocalDate.now())
                .importe(importe)
                .metodoPago(metodo)
                .observaciones(observaciones)
                .build();
        p.setDevolucion(dev);
        pedidoRepository.save(p);
    }

    @Transactional
    public void eliminarDevolucion(Long pedidoId) {
        PedidoProducto p = findById(pedidoId);
        if (p.getDevolucion() == null) {
            throw new IllegalStateException("Este pedido no tiene devolucion registrada");
        }
        p.setDevolucion(null);
        pedidoRepository.save(p);
    }

    // ------------------------------------------------------------
    // UTILIDADES
    // ------------------------------------------------------------
    public PedidoProductoForm toForm(PedidoProducto p) {
        PedidoProductoForm form = new PedidoProductoForm();
        form.setId(p.getId());
        form.setProductoId(p.getProducto().getId());
        form.setAlumnoId(p.getAlumno().getId());
        form.setCantidad(p.getCantidad());
        form.setTalla(p.getTalla());
        form.setMetodoPago(p.getMetodoPago());
        form.setObservaciones(p.getObservaciones());
        return form;
    }
}