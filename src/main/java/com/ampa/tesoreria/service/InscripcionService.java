package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.InscripcionForm;
import com.ampa.tesoreria.model.*;
import com.ampa.tesoreria.model.enums.*;
import com.ampa.tesoreria.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final PagoMensualRepository pagoMensualRepository;
    private final DevolucionFianzaRepository devolucionFianzaRepository;
    private final AlumnoService alumnoService;
    private final ActividadService actividadService;

    // ------------------------------------------------------------
    // LISTADOS
    // ------------------------------------------------------------
    public List<Inscripcion> listar(Long actividadId, String q, EstadoInscripcion estadoFiltro) {
        List<Inscripcion> inscripciones = inscripcionRepository.findByActividadId(actividadId);

        // Filtro por texto (nombre/curso del alumno)
        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            inscripciones = inscripciones.stream()
                    .filter(i -> {
                        Alumno al = i.getAlumno();
                        return (al.getNombre() + " " + al.getApellidos()).toLowerCase().contains(query)
                                || (al.getCurso() != null && al.getCurso().toLowerCase().contains(query));
                    })
                    .toList();
        }

        // Filtro por estado
        if (estadoFiltro != null) {
            inscripciones = inscripciones.stream()
                    .filter(i -> i.getEstado() == estadoFiltro)
                    .toList();
        }

        return inscripciones;
    }

    public Inscripcion findById(Long id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada: " + id));
    }

    public List<PagoMensual> pagosDe(Long inscripcionId) {
        return pagoMensualRepository.findByInscripcionIdOrderByMesAsc(inscripcionId);
    }

    // ------------------------------------------------------------
    // INSCRIBIR ALUMNO
    // ------------------------------------------------------------
    @Transactional
    public Inscripcion inscribir(InscripcionForm form) {
        Actividad actividad = actividadService.findById(form.getActividadId());
        Alumno alumno = alumnoService.findById(form.getAlumnoId());

        if (inscripcionRepository.existsByActividadIdAndAlumnoId(actividad.getId(), alumno.getId())) {
            throw new IllegalArgumentException(
                    "El alumno " + alumno.getNombreCompleto() + " ya esta inscrito en esta actividad");
        }
        if (form.getModalidad() == null) {
            throw new IllegalArgumentException("Debes elegir una modalidad");
        }

        // Validar modalidades segun tipo de actividad
        if (actividad.getTipo() == TipoActividad.EXTRAESCOLAR && form.getModalidad() == Modalidad.POR_DIAS) {
            throw new IllegalArgumentException(
                    "Las extraescolares no admiten la modalidad POR_DIAS, solo COMPLETO o MEDIO");
        }
        if (form.getModalidad() == Modalidad.POR_DIAS
                && (form.getDiasAsistidos() == null || form.getDiasAsistidos() <= 0)) {
            throw new IllegalArgumentException("Debes indicar el numero de dias para la modalidad POR_DIAS");
        }

        // Calcular importe base
        BigDecimal importeBase = actividadService.calcularImporteBase(
                actividad, form.getModalidad(), form.getDiasAsistidos());
        if (importeBase == null) {
            throw new IllegalArgumentException(
                    "La actividad no tiene configurado el precio para la modalidad " + form.getModalidad());
        }

        // Aplicar descuento socio solo si el alumno pertenece a un socio
        BigDecimal importeCalculado;
        BigDecimal fianzaImporte;
        if (alumno.getSocio() != null) {
            importeCalculado = actividadService.aplicarDescuento(actividad, importeBase);
            fianzaImporte = actividadService.calcularFianza(actividad);
        } else {
            importeCalculado = importeBase.setScale(2, RoundingMode.HALF_UP);
            fianzaImporte = actividad.isTieneFianza() ? actividad.getImporteFianza() : null;
        }

        Inscripcion inscripcion = Inscripcion.builder()
                .actividad(actividad)
                .alumno(alumno)
                .modalidad(form.getModalidad())
                .diasAsistidos(form.getModalidad() == Modalidad.POR_DIAS ? form.getDiasAsistidos() : null)
                .diasSemanaSeleccionados(form.getModalidad() == Modalidad.POR_DIAS
                        ? form.getDiasSemanaSeleccionados() : null)
                .importeMensualCalculado(importeCalculado)
                .permisoImagen(form.getPermisoImagen())
                .fechaInscripcion(LocalDate.now())
                .observaciones(form.getObservaciones())
                .estado(EstadoInscripcion.ACTIVA)
                .fianzaImporte(fianzaImporte)
                .fianzaEstado(fianzaImporte != null ? EstadoFianza.PENDIENTE : null)
                .pagosMensuales(new ArrayList<>())
                .build();

        generarPagosMensuales(inscripcion, actividad, importeCalculado);
        return inscripcionRepository.save(inscripcion);
    }

    /**
     * Genera un PagoMensual por cada mes entre fechaInicio y fechaFin de la actividad.
     * Todos con el mismo importe y pagado=false.
     */
    private void generarPagosMensuales(Inscripcion inscripcion, Actividad actividad, BigDecimal importeMensual) {
        YearMonth desde = YearMonth.from(actividad.getFechaInicio());
        YearMonth hasta = YearMonth.from(actividad.getFechaFin());

        YearMonth actual = desde;
        while (!actual.isAfter(hasta)) {
            PagoMensual pago = PagoMensual.builder()
                    .inscripcion(inscripcion)
                    .mes(actual)
                    .importe(importeMensual)
                    .pagado(false)
                    .cubiertoPorFianza(false)
                    .build();
            inscripcion.addPagoMensual(pago);
            actual = actual.plusMonths(1);
        }
    }

    // ------------------------------------------------------------
    // ACTUALIZAR INSCRIPCIÓN (solo observaciones / permiso imagen / modalidad si no hay pagos)
    // ------------------------------------------------------------
    @Transactional
    public Inscripcion actualizar(Long id, InscripcionForm form) {
        Inscripcion i = findById(id);

        // Si cambia la modalidad, recalcular importe y actualizar pagos pendientes
        if (form.getModalidad() != null && form.getModalidad() != i.getModalidad()) {
            Actividad actividad = i.getActividad();

            if (actividad.getTipo() == TipoActividad.EXTRAESCOLAR && form.getModalidad() == Modalidad.POR_DIAS) {
                throw new IllegalArgumentException("Las extraescolares no admiten POR_DIAS");
            }

            BigDecimal importeBase = actividadService.calcularImporteBase(
                    actividad, form.getModalidad(), form.getDiasAsistidos());
            BigDecimal nuevoImporte = (i.getAlumno().getSocio() != null)
                    ? actividadService.aplicarDescuento(actividad, importeBase)
                    : importeBase.setScale(2, RoundingMode.HALF_UP);

            i.setModalidad(form.getModalidad());
            i.setDiasAsistidos(form.getModalidad() == Modalidad.POR_DIAS ? form.getDiasAsistidos() : null);
            i.setDiasSemanaSeleccionados(form.getModalidad() == Modalidad.POR_DIAS
                    ? form.getDiasSemanaSeleccionados() : null);
            i.setImporteMensualCalculado(nuevoImporte);

            for (PagoMensual p : i.getPagosMensuales()) {
                if (!p.isPagado() && !p.isCubiertoPorFianza()) {
                    p.setImporte(nuevoImporte);
                }
            }
        }

        i.setObservaciones(form.getObservaciones());
        i.setPermisoImagen(form.getPermisoImagen());
        return inscripcionRepository.save(i);
    }

    // ------------------------------------------------------------
    // BAJA (sin borrar)
    // ------------------------------------------------------------
    @Transactional
    public void darDeBaja(Long id) {
        Inscripcion i = findById(id);
        i.setEstado(EstadoInscripcion.BAJA);
        inscripcionRepository.save(i);
    }

    @Transactional
    public void reactivar(Long id) {
        Inscripcion i = findById(id);
        i.setEstado(EstadoInscripcion.ACTIVA);
        inscripcionRepository.save(i);
    }

    // ------------------------------------------------------------
    // PAGOS MENSUALES
    // ------------------------------------------------------------
    @Transactional
    public void marcarMesPagado(Long inscripcionId, Long pagoId, MetodoPago metodo, LocalDate fecha) {
        Inscripcion i = findById(inscripcionId);
        PagoMensual p = i.getPagosMensuales().stream()
                .filter(pm -> pm.getId().equals(pagoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
        p.setPagado(true);
        p.setMetodoPago(metodo);
        p.setFechaPago(fecha != null ? fecha : LocalDate.now());
        pagoMensualRepository.save(p);
    }

    @Transactional
    public void desmarcarMesPagado(Long inscripcionId, Long pagoId) {
        Inscripcion i = findById(inscripcionId);
        PagoMensual p = i.getPagosMensuales().stream()
                .filter(pm -> pm.getId().equals(pagoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
        p.setPagado(false);
        p.setFechaPago(null);
        p.setMetodoPago(null);
        pagoMensualRepository.save(p);
    }

    // ------------------------------------------------------------
    // FIANZA
    // ------------------------------------------------------------
    @Transactional
    public void marcarFianzaPagada(Long inscripcionId, MetodoPago metodo, LocalDate fecha) {
        Inscripcion i = findById(inscripcionId);
        if (i.getFianzaImporte() == null) {
            throw new IllegalStateException("Esta inscripcion no tiene fianza");
        }
        i.setFianzaEstado(EstadoFianza.PAGADA);
        i.setFianzaMetodoPago(metodo);
        i.setFianzaFechaPago(fecha != null ? fecha : LocalDate.now());
        inscripcionRepository.save(i);
    }

    @Transactional
    public void devolverFianza(Long inscripcionId, LocalDate fecha, BigDecimal importe,
                               MetodoPago metodo, String observaciones) {
        Inscripcion i = findById(inscripcionId);
        if (i.getFianzaEstado() != EstadoFianza.PAGADA) {
            throw new IllegalStateException("Solo se puede devolver una fianza que este pagada");
        }
        if (i.getDevolucionFianza() != null) {
            throw new IllegalStateException("Esta fianza ya fue devuelta anteriormente");
        }

        DevolucionFianza dev = DevolucionFianza.builder()
                .fecha(fecha != null ? fecha : LocalDate.now())
                .importe(importe != null ? importe : i.getFianzaImporte())
                .metodoPago(metodo)
                .observaciones(observaciones)
                .build();
        i.setDevolucionFianza(dev);
        i.setFianzaEstado(EstadoFianza.DEVUELTA);
        inscripcionRepository.save(i);
    }

    @Transactional
    public void aplicarFianza(Long inscripcionId) {
        Inscripcion i = findById(inscripcionId);
        if (i.getFianzaEstado() != EstadoFianza.PAGADA) {
            throw new IllegalStateException("Solo se puede aplicar una fianza que este pagada");
        }
        if (i.getPagosMensuales().isEmpty()) {
            throw new IllegalStateException("Esta inscripcion no tiene pagos mensuales");
        }

        // El último mes del periodo queda cubierto por la fianza
        PagoMensual ultimo = i.getPagosMensuales().get(i.getPagosMensuales().size() - 1);
        if (ultimo.isPagado()) {
            throw new IllegalStateException("El ultimo mes ya esta pagado, no hace falta aplicar la fianza");
        }
        ultimo.setPagado(true);
        ultimo.setCubiertoPorFianza(true);
        ultimo.setFechaPago(LocalDate.now());
        pagoMensualRepository.save(ultimo);

        i.setFianzaEstado(EstadoFianza.APLICADA);
        inscripcionRepository.save(i);
    }

    // ------------------------------------------------------------
    // CÁLCULOS DE TOTALES
    // ------------------------------------------------------------
    public BigDecimal totalPagado(Inscripcion i) {
        return i.getPagosMensuales().stream()
                .filter(PagoMensual::isPagado)
                .map(PagoMensual::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPendiente(Inscripcion i) {
        return i.getPagosMensuales().stream()
                .filter(p -> !p.isPagado())
                .map(PagoMensual::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}