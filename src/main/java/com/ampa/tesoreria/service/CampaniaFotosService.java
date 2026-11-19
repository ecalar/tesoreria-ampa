package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.CampaniaFotosForm;
import com.ampa.tesoreria.model.CampaniaFotos;
import com.ampa.tesoreria.model.PedidoFoto;
import com.ampa.tesoreria.repository.CampaniaFotosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaniaFotosService {

    private final CampaniaFotosRepository campaniaRepository;
    private final CursoService cursoService;
    private final AjusteService ajusteService;

    // ------------------------------------------------------------
    // LISTADOS
    // ------------------------------------------------------------
    public List<CampaniaFotos> listar(String q) {
        Long cursoId = cursoService.getCursoActivo().getId();
        List<CampaniaFotos> campanias = campaniaRepository.findByCursoId(cursoId);
        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            campanias = campanias.stream()
                    .filter(c -> c.getNombre().toLowerCase().contains(query))
                    .toList();
        }
        return campanias;
    }

    public CampaniaFotos findById(Long id) {
        return campaniaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaña no encontrada: " + id));
    }

    // ------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------
    @Transactional
    public CampaniaFotos crear(CampaniaFotosForm form) {
        validar(form);
        CampaniaFotos campania = CampaniaFotos.builder()
                .curso(cursoService.getCursoActivo())
                .nombre(form.getNombre())
                .descripcion(form.getDescripcion())
                .fechaInicio(form.getFechaInicio())
                .fechaFin(form.getFechaFin())
                .precioLote(form.getPrecioLote())
                .precioGrupal(form.getPrecioGrupal())
                .precioIndividual(form.getPrecioIndividual())
                .descuentoSocioPorcentaje(form.getDescuentoSocioPorcentaje() != null
                        ? form.getDescuentoSocioPorcentaje() : BigDecimal.ZERO)
                .cerrada(false)
                .build();
        return campaniaRepository.save(campania);
    }

    @Transactional
    public CampaniaFotos actualizar(Long id, CampaniaFotosForm form) {
        validar(form);
        CampaniaFotos c = findById(id);
        c.setNombre(form.getNombre());
        c.setDescripcion(form.getDescripcion());
        c.setFechaInicio(form.getFechaInicio());
        c.setFechaFin(form.getFechaFin());
        c.setPrecioLote(form.getPrecioLote());
        c.setPrecioGrupal(form.getPrecioGrupal());
        c.setPrecioIndividual(form.getPrecioIndividual());
        c.setDescuentoSocioPorcentaje(form.getDescuentoSocioPorcentaje() != null
                ? form.getDescuentoSocioPorcentaje() : BigDecimal.ZERO);
        return campaniaRepository.save(c);
    }

    @Transactional
    public void cerrar(Long id) {
        CampaniaFotos c = findById(id);
        c.setCerrada(true);
        campaniaRepository.save(c);
    }

    @Transactional
    public void reabrir(Long id) {
        CampaniaFotos c = findById(id);
        c.setCerrada(false);
        campaniaRepository.save(c);
    }

    @Transactional
    public void eliminar(Long id) {
        CampaniaFotos c = findById(id);
        if (!c.getPedidos().isEmpty()) {
            throw new IllegalStateException(
                    "No se puede borrar una campaña con pedidos. Bórralos primero.");
        }
        campaniaRepository.delete(c);
    }

    // ------------------------------------------------------------
    // UTILIDADES DE FORMULARIO
    // ------------------------------------------------------------
    public CampaniaFotosForm toForm(CampaniaFotos c) {
        CampaniaFotosForm form = new CampaniaFotosForm();
        form.setId(c.getId());
        form.setNombre(c.getNombre());
        form.setDescripcion(c.getDescripcion());
        form.setFechaInicio(c.getFechaInicio());
        form.setFechaFin(c.getFechaFin());
        form.setPrecioLote(c.getPrecioLote());
        form.setPrecioGrupal(c.getPrecioGrupal());
        form.setPrecioIndividual(c.getPrecioIndividual());
        form.setDescuentoSocioPorcentaje(c.getDescuentoSocioPorcentaje());
        return form;
    }

    /** Formulario por defecto para nueva campaña, con el descuento global como sugerencia */
    public CampaniaFotosForm nuevoFormulario() {
        CampaniaFotosForm form = new CampaniaFotosForm();
        form.setDescuentoSocioPorcentaje(ajusteService.getDescuentoSocioGlobal());
        form.setPrecioLote(BigDecimal.ZERO);
        form.setPrecioGrupal(BigDecimal.ZERO);
        form.setPrecioIndividual(BigDecimal.ZERO);
        return form;
    }

    // ------------------------------------------------------------
    // CÁLCULOS
    // ------------------------------------------------------------
    public BigDecimal totalRecaudado(CampaniaFotos c) {
        return c.getPedidos().stream()
                .map(PedidoFoto::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCobrado(CampaniaFotos c) {
        return c.getPedidos().stream()
                .filter(PedidoFoto::isPagado)
                .map(PedidoFoto::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPendiente(CampaniaFotos c) {
        return c.getPedidos().stream()
                .filter(p -> !p.isPagado())
                .map(PedidoFoto::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalDevuelto(CampaniaFotos c) {
        // Se calculará cuando añadamos DevolucionPedidoFoto en la 8.2
        // De momento 0
        return BigDecimal.ZERO;
    }

    // ------------------------------------------------------------
    private void validar(CampaniaFotosForm form) {
        if (form.getFechaInicio() == null || form.getFechaFin() == null) {
            throw new IllegalArgumentException("Las fechas son obligatorias");
        }
        if (form.getFechaFin().isBefore(form.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        if (form.getPrecioLote() == null || form.getPrecioGrupal() == null
                || form.getPrecioIndividual() == null) {
            throw new IllegalArgumentException("Los tres precios son obligatorios");
        }
    }
}