package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.ActividadForm;
import com.ampa.tesoreria.model.Actividad;
import com.ampa.tesoreria.model.Inscripcion;
import com.ampa.tesoreria.model.enums.EstadoInscripcion;
import com.ampa.tesoreria.model.enums.TipoActividad;
import com.ampa.tesoreria.repository.ActividadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActividadService {

    private final ActividadRepository actividadRepository;
    private final CursoService cursoService;

    // ------------------------------------------------------------
    // LISTADOS Y BÚSQUEDA
    // ------------------------------------------------------------
    public List<Actividad> listarExtraescolares(String q) {
        Long cursoId = cursoService.getCursoActivo().getId();
        if (q != null && !q.isBlank()) {
            return actividadRepository.findByCursoIdAndNombreContainingIgnoreCase(cursoId, q);
        }
        return actividadRepository.findByCursoIdAndTipo(cursoId, TipoActividad.EXTRAESCOLAR);
    }

    public Actividad findById(Long id) {
        return actividadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada: " + id));
    }

    // ------------------------------------------------------------
    // CREAR / EDITAR / BORRAR
    // ------------------------------------------------------------
    @Transactional
    public Actividad crear(ActividadForm form) {
        validarFechas(form);
        Actividad actividad = Actividad.builder()
                .curso(cursoService.getCursoActivo())
                .tipo(TipoActividad.EXTRAESCOLAR)
                .nombre(form.getNombre())
                .descripcion(form.getDescripcion())
                .fechaInicio(form.getFechaInicio())
                .fechaFin(form.getFechaFin())
                .diasSemana(joinDias(form.getDiasSeleccionados()))
                .horario(form.getHorario())
                .precioMesCompleto(form.getPrecioMesCompleto())
                .precioMedioMes(form.getPrecioMedioMes())
                .descuentoSocioPorcentaje(form.getDescuentoSocioPorcentaje())
                .tieneFianza(form.isTieneFianza())
                .importeFianza(form.isTieneFianza() ? form.getImporteFianza() : null)
                .build();
        return actividadRepository.save(actividad);
    }

    @Transactional
    public Actividad actualizar(Long id, ActividadForm form) {
        validarFechas(form);
        Actividad a = findById(id);
        a.setNombre(form.getNombre());
        a.setDescripcion(form.getDescripcion());
        a.setFechaInicio(form.getFechaInicio());
        a.setFechaFin(form.getFechaFin());
        a.setDiasSemana(joinDias(form.getDiasSeleccionados()));
        a.setHorario(form.getHorario());
        a.setPrecioMesCompleto(form.getPrecioMesCompleto());
        a.setPrecioMedioMes(form.getPrecioMedioMes());
        a.setDescuentoSocioPorcentaje(form.getDescuentoSocioPorcentaje());
        a.setTieneFianza(form.isTieneFianza());
        a.setImporteFianza(form.isTieneFianza() ? form.getImporteFianza() : null);
        return actividadRepository.save(a);
    }

    @Transactional
    public void eliminar(Long id) {
        Actividad a = findById(id);
        long inscritos = a.getInscripciones().stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.ACTIVA)
                .count();
        if (inscritos > 0) {
            throw new IllegalStateException(
                    "No se puede borrar una actividad con " + inscritos + " inscripciones activas. " +
                            "Da de baja a los alumnos primero.");
        }
        actividadRepository.delete(a);
    }

    // ------------------------------------------------------------
    // UTILIDADES DE FORMULARIO
    // ------------------------------------------------------------
    public ActividadForm toForm(Actividad a) {
        ActividadForm form = new ActividadForm();
        form.setId(a.getId());
        form.setNombre(a.getNombre());
        form.setDescripcion(a.getDescripcion());
        form.setFechaInicio(a.getFechaInicio());
        form.setFechaFin(a.getFechaFin());
        if (a.getDiasSemana() != null) {
            form.setDiasSeleccionados(Arrays.asList(a.getDiasSemana().split(",")));
        }
        form.setHorario(a.getHorario());
        form.setPrecioMesCompleto(a.getPrecioMesCompleto());
        form.setPrecioMedioMes(a.getPrecioMedioMes());
        form.setDescuentoSocioPorcentaje(a.getDescuentoSocioPorcentaje());
        form.setTieneFianza(a.isTieneFianza());
        form.setImporteFianza(a.getImporteFianza());
        return form;
    }

    private void validarFechas(ActividadForm form) {
        if (form.getFechaInicio() == null || form.getFechaFin() == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (form.getFechaFin().isBefore(form.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio");
        }
    }

    private String joinDias(List<String> dias) {
        if (dias == null || dias.isEmpty()) return null;
        return String.join(",", dias);
    }

    // ------------------------------------------------------------
    // CÁLCULOS
    // ------------------------------------------------------------

    /** Importe mensual de una inscripción aplicando el descuento socio de la actividad */
    public BigDecimal calcularImporteMensual(Actividad actividad, BigDecimal importeBase) {
        BigDecimal descuento = actividad.getDescuentoSocioPorcentaje() == null
                ? BigDecimal.ZERO
                : actividad.getDescuentoSocioPorcentaje();
        BigDecimal factor = BigDecimal.ONE.subtract(
                descuento.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return importeBase.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /** Fianza: precio mes completo con descuento aplicado (snapshot al inscribir) */
    public BigDecimal calcularFianza(Actividad actividad) {
        if (!actividad.isTieneFianza() || actividad.getImporteFianza() == null) {
            return null;
        }
        return calcularImporteMensual(actividad, actividad.getImporteFianza());
    }

    /** Nº de inscripciones activas */
    public long countInscripcionesActivas(Actividad a) {
        return a.getInscripciones().stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.ACTIVA)
                .count();
    }

    /** Total mensual facturado (suma de importes mensuales de inscripciones activas) */
    public BigDecimal totalMensualActivo(Actividad a) {
        return a.getInscripciones().stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.ACTIVA)
                .map(Inscripcion::getImporteMensualCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}