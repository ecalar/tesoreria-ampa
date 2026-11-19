package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.PedidoFotoForm;
import com.ampa.tesoreria.model.*;
import com.ampa.tesoreria.repository.PedidoFotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoFotoService {

    private final PedidoFotoRepository pedidoRepository;
    private final CampaniaFotosService campaniaService;
    private final AlumnoService alumnoService;

    // ------------------------------------------------------------
    // LISTADOS
    // ------------------------------------------------------------
    public List<PedidoFoto> listarPorCampania(Long campaniaId, String q, Boolean pagado) {
        List<PedidoFoto> pedidos = pedidoRepository.findByCampaniaId(campaniaId);
        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            pedidos = pedidos.stream()
                    .filter(p -> p.getAlumno().getNombreCompleto().toLowerCase().contains(query)
                            || (p.getCursoAlumno() != null
                            && p.getCursoAlumno().toLowerCase().contains(query)))
                    .toList();
        }
        if (pagado != null) {
            pedidos = pedidos.stream()
                    .filter(p -> p.isPagado() == pagado)
                    .toList();
        }
        return pedidos;
    }

    public PedidoFoto findById(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
    }

    // ------------------------------------------------------------
    // CREAR
    // ------------------------------------------------------------
    @Transactional
    public PedidoFoto crear(PedidoFotoForm form) {
        CampaniaFotos campania = campaniaService.findById(form.getCampaniaId());
        if (campania.isCerrada()) {
            throw new IllegalStateException("La campaña está cerrada");
        }
        Alumno alumno = alumnoService.findById(form.getAlumnoId());

        if (pedidoRepository.findByCampaniaIdAndAlumnoId(campania.getId(), alumno.getId()).isPresent()) {
            throw new IllegalArgumentException(
                    "El alumno " + alumno.getNombreCompleto() + " ya tiene un pedido en esta campaña");
        }

        // Validar cantidades mínimas
        int numLotes = form.getNumLotes() != null ? form.getNumLotes() : 0;
        int numIndividuales = form.getNumIndividuales() != null ? form.getNumIndividuales() : 0;
        int totalGrupales = form.getGrupos() == null ? 0
                : form.getGrupos().stream()
                .filter(g -> g.getCantidad() != null && g.getCantidad() > 0)
                .mapToInt(PedidoFotoForm.GrupoForm::getCantidad).sum();

        if (numLotes == 0 && numIndividuales == 0 && totalGrupales == 0) {
            throw new IllegalArgumentException("El pedido debe tener al menos un lote, grupal o individual");
        }

        // Calcular totales con los precios de la campaña
        BigDecimal precioLote = campania.getPrecioLote();
        BigDecimal precioGrupal = campania.getPrecioGrupal();
        BigDecimal precioIndividual = campania.getPrecioIndividual();
        BigDecimal descuento = campania.getDescuentoSocioPorcentaje() != null
                ? campania.getDescuentoSocioPorcentaje() : BigDecimal.ZERO;

        BigDecimal subtotal = BigDecimal.ZERO;
        subtotal = subtotal.add(precioLote.multiply(BigDecimal.valueOf(numLotes)));
        subtotal = subtotal.add(precioGrupal.multiply(BigDecimal.valueOf(totalGrupales)));
        subtotal = subtotal.add(precioIndividual.multiply(BigDecimal.valueOf(numIndividuales)));

        // Descuento solo si el alumno tiene socio
        BigDecimal descuentoAplicado = (alumno.getSocio() != null) ? descuento : BigDecimal.ZERO;

        BigDecimal total = aplicarDescuento(subtotal, descuentoAplicado);

        PedidoFoto pedido = PedidoFoto.builder()
                .campania(campania)
                .alumno(alumno)
                .cursoAlumno(alumno.getCurso())
                .numLotes(numLotes)
                .numIndividuales(numIndividuales)
                .precioLoteAplicado(precioLote)
                .precioGrupalAplicado(precioGrupal)
                .precioIndividualAplicado(precioIndividual)
                .descuentoAplicado(descuentoAplicado)
                .subtotal(subtotal)
                .total(total)
                .metodoPago(form.getMetodoPago())
                .pagado(false)
                .fechaPedido(LocalDate.now())
                .observaciones(form.getObservaciones())
                .grupos(new ArrayList<>())
                .hermanos(new ArrayList<>())
                .build();

        // Añadir grupos (solo los que tienen cantidad > 0)
        if (form.getGrupos() != null) {
            for (PedidoFotoForm.GrupoForm g : form.getGrupos()) {
                if (g.getCantidad() != null && g.getCantidad() > 0
                        && g.getCurso() != null && !g.getCurso().isBlank()) {
                    GrupoFoto grupo = GrupoFoto.builder()
                            .curso(g.getCurso())
                            .cantidad(g.getCantidad())
                            .build();
                    pedido.addGrupo(grupo);
                }
            }
        }

        // Añadir hermanos (solo los que tienen nombre)
        if (form.getHermanos() != null) {
            for (PedidoFotoForm.HermanoForm h : form.getHermanos()) {
                if (h.getNombre() != null && !h.getNombre().isBlank()) {
                    HermanoFoto hermano = HermanoFoto.builder()
                            .nombre(h.getNombre())
                            .curso(h.getCurso())
                            .build();
                    pedido.addHermano(hermano);
                }
            }
        }

        return pedidoRepository.save(pedido);
    }

    // ------------------------------------------------------------
    // ACTUALIZAR (solo si no está pagado)
    // ------------------------------------------------------------
    @Transactional
    public PedidoFoto actualizar(Long id, PedidoFotoForm form) {
        PedidoFoto pedido = findById(id);
        if (pedido.isPagado()) {
            throw new IllegalStateException("No se puede modificar un pedido ya pagado. " +
                    "Desmárcalo primero si necesitas cambiarlo.");
        }

        int numLotes = form.getNumLotes() != null ? form.getNumLotes() : 0;
        int numIndividuales = form.getNumIndividuales() != null ? form.getNumIndividuales() : 0;
        int totalGrupales = form.getGrupos() == null ? 0
                : form.getGrupos().stream()
                .filter(g -> g.getCantidad() != null && g.getCantidad() > 0)
                .mapToInt(PedidoFotoForm.GrupoForm::getCantidad).sum();

        if (numLotes == 0 && numIndividuales == 0 && totalGrupales == 0) {
            throw new IllegalArgumentException("El pedido debe tener al menos un lote, grupal o individual");
        }

        // Recalcular con los precios guardados (snapshot) para no romper el histórico
        BigDecimal subtotal = BigDecimal.ZERO;
        subtotal = subtotal.add(pedido.getPrecioLoteAplicado().multiply(BigDecimal.valueOf(numLotes)));
        subtotal = subtotal.add(pedido.getPrecioGrupalAplicado().multiply(BigDecimal.valueOf(totalGrupales)));
        subtotal = subtotal.add(pedido.getPrecioIndividualAplicado().multiply(BigDecimal.valueOf(numIndividuales)));

        BigDecimal total = aplicarDescuento(subtotal, pedido.getDescuentoAplicado());

        pedido.setNumLotes(numLotes);
        pedido.setNumIndividuales(numIndividuales);
        pedido.setSubtotal(subtotal);
        pedido.setTotal(total);
        pedido.setMetodoPago(form.getMetodoPago());
        pedido.setObservaciones(form.getObservaciones());

        // Reemplazar grupos
        pedido.getGrupos().clear();
        if (form.getGrupos() != null) {
            for (PedidoFotoForm.GrupoForm g : form.getGrupos()) {
                if (g.getCantidad() != null && g.getCantidad() > 0
                        && g.getCurso() != null && !g.getCurso().isBlank()) {
                    pedido.addGrupo(GrupoFoto.builder()
                            .curso(g.getCurso())
                            .cantidad(g.getCantidad())
                            .build());
                }
            }
        }

        // Reemplazar hermanos
        pedido.getHermanos().clear();
        if (form.getHermanos() != null) {
            for (PedidoFotoForm.HermanoForm h : form.getHermanos()) {
                if (h.getNombre() != null && !h.getNombre().isBlank()) {
                    pedido.addHermano(HermanoFoto.builder()
                            .nombre(h.getNombre())
                            .curso(h.getCurso())
                            .build());
                }
            }
        }

        return pedidoRepository.save(pedido);
    }

    // ------------------------------------------------------------
    // MARCAR PAGADO / DESMARCAR
    // ------------------------------------------------------------
    @Transactional
    public void marcarPagado(Long id, LocalDate fecha) {
        PedidoFoto p = findById(id);
        p.setPagado(true);
        p.setFechaPago(fecha != null ? fecha : LocalDate.now());
        pedidoRepository.save(p);
    }

    @Transactional
    public void desmarcarPagado(Long id) {
        PedidoFoto p = findById(id);
        p.setPagado(false);
        p.setFechaPago(null);
        pedidoRepository.save(p);
    }

    @Transactional
    public void eliminar(Long id) {
        pedidoRepository.deleteById(id);
    }

    // ------------------------------------------------------------
    // UTILIDADES
    // ------------------------------------------------------------
    public PedidoFotoForm toForm(PedidoFoto p) {
        PedidoFotoForm form = new PedidoFotoForm();
        form.setId(p.getId());
        form.setCampaniaId(p.getCampania().getId());
        form.setAlumnoId(p.getAlumno().getId());
        form.setNumLotes(p.getNumLotes());
        form.setNumIndividuales(p.getNumIndividuales());
        form.setMetodoPago(p.getMetodoPago());
        form.setObservaciones(p.getObservaciones());

        if (p.getGrupos() != null) {
            for (GrupoFoto g : p.getGrupos()) {
                PedidoFotoForm.GrupoForm gf = new PedidoFotoForm.GrupoForm();
                gf.setCurso(g.getCurso());
                gf.setCantidad(g.getCantidad());
                form.getGrupos().add(gf);
            }
        }
        if (p.getHermanos() != null) {
            for (HermanoFoto h : p.getHermanos()) {
                PedidoFotoForm.HermanoForm hf = new PedidoFotoForm.HermanoForm();
                hf.setNombre(h.getNombre());
                hf.setCurso(h.getCurso());
                form.getHermanos().add(hf);
            }
        }
        return form;
    }

    // ------------------------------------------------------------
    private BigDecimal aplicarDescuento(BigDecimal base, BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.signum() == 0) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.subtract(
                porcentaje.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return base.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}