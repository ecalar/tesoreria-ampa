package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.ActividadForm;
import com.ampa.tesoreria.model.Actividad;
import com.ampa.tesoreria.model.Inscripcion;
import com.ampa.tesoreria.model.enums.EstadoInscripcion;
import com.ampa.tesoreria.model.enums.Modalidad;
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
    // LISTADOS
    // ------------------------------------------------------------
    public List<Actividad> listarExtraescolares(String q) {
        return listarPorTipo(TipoActividad.EXTRAESCOLAR, q);
    }

    public List<Actividad> listarEscuelas(String q) {
        return listarPorTipo(TipoActividad.ESCUELA, q);
    }

    public List<Actividad> listarPorTipo(TipoActividad tipo, String q) {
        Long cursoId = cursoService.getCursoActivo().getId();
        if (q != null && !q.isBlank()) {
            List<Actividad> todos = actividadRepository.findByCursoIdAndNombreContainingIgnoreCase(cursoId, q);
            return todos.stream().filter(a -> a.getTipo() == tipo).toList();
        }
        return actividadRepository.findByCursoIdAndTipo(cursoId, tipo);
    }

    public Actividad findById(Long id) {
        return actividadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada: " + id));
    }

    // ------------------------------------------------------------
    // CREAR / EDITAR / BORRAR
    // ------------------------------------------------------------
    @Transactional
    public Actividad crear(TipoActividad tipo, ActividadForm form) {
        validarFechas(form);
        Actividad actividad = Actividad.builder()
                .curso(cursoService.getCursoActivo())
                .tipo(tipo)
                .nombre(form.getNombre())
                .descripcion(form.getDescripcion())
                .fechaInicio(form.getFechaInicio())
                .fechaFin(form.getFechaFin())
                .diasSemana(tipo == TipoActividad.EXTRA_ACTIVIDAD ? null : joinDias(form.getDiasSeleccionados()))
                .horario(form.getHorario())
                .precioMesCompleto(form.getPrecioMesCompleto())
                .precioMedioMes(tipo == TipoActividad.EXTRA_ACTIVIDAD ? null : form.getPrecioMedioMes())
                .precioPorDia(tipo != TipoActividad.EXTRAESCOLAR ? form.getPrecioPorDia() : null)
                .descuentoSocioPorcentaje(form.getDescuentoSocioPorcentaje() != null
                        ? form.getDescuentoSocioPorcentaje() : BigDecimal.ZERO)
                // La fianza SOLO se persiste si es extraescolar
                .tieneFianza(tipo == TipoActividad.EXTRAESCOLAR && form.isTieneFianza())
                .importeFianza(tipo == TipoActividad.EXTRAESCOLAR && form.isTieneFianza()
                        ? form.getImporteFianza() : null)
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
        a.setDiasSemana(a.getTipo() == TipoActividad.EXTRA_ACTIVIDAD
                ? null
                : joinDias(form.getDiasSeleccionados()));
        a.setHorario(form.getHorario());
        a.setPrecioMesCompleto(form.getPrecioMesCompleto());
        a.setPrecioMedioMes(a.getTipo() == TipoActividad.EXTRA_ACTIVIDAD
                ? null
                : form.getPrecioMedioMes());
        a.setPrecioPorDia(a.getTipo() != TipoActividad.EXTRAESCOLAR ? form.getPrecioPorDia() : null);
        a.setDescuentoSocioPorcentaje(form.getDescuentoSocioPorcentaje() != null
                ? form.getDescuentoSocioPorcentaje() : BigDecimal.ZERO);

        // Solo extraescolares manejan fianza
        if (a.getTipo() == TipoActividad.EXTRAESCOLAR) {
            a.setTieneFianza(form.isTieneFianza());
            a.setImporteFianza(form.isTieneFianza() ? form.getImporteFianza() : null);
        } else {
            a.setTieneFianza(false);
            a.setImporteFianza(null);
        }

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
    // FORMULARIO
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
        form.setPrecioPorDia(a.getPrecioPorDia());
        form.setDescuentoSocioPorcentaje(a.getDescuentoSocioPorcentaje());
        form.setTieneFianza(a.isTieneFianza());
        form.setImporteFianza(a.getImporteFianza());
        return form;
    }

    // ------------------------------------------------------------
    // CÁLCULOS
    // ------------------------------------------------------------

    /** Calcula el importe base segun modalidad y dias (sin aplicar descuento) */
    public BigDecimal calcularImporteBase(Actividad actividad, Modalidad modalidad, Integer diasAsistidos) {
        return switch (modalidad) {
            case COMPLETO -> {
                if (actividad.getPrecioMesCompleto() == null) {
                    throw new IllegalArgumentException("La actividad no tiene precio completo configurado");
                }
                yield actividad.getPrecioMesCompleto();
            }
            case MEDIO -> {
                if (actividad.getPrecioMedioMes() == null) {
                    throw new IllegalArgumentException(
                            "Esta actividad no admite la modalidad MEDIO. Elige COMPLETO o POR_DIAS.");
                }
                yield actividad.getPrecioMedioMes();
            }
            case POR_DIAS -> {
                if (actividad.getPrecioPorDia() == null) {
                    throw new IllegalArgumentException("La actividad no tiene configurado el precio por dia");
                }
                if (diasAsistidos == null || diasAsistidos <= 0) {
                    throw new IllegalArgumentException("Indica el numero de dias para la modalidad POR_DIAS");
                }
                yield actividad.getPrecioPorDia().multiply(BigDecimal.valueOf(diasAsistidos));
            }
        };
    }

    /** Aplica el descuento socio a un importe base */
    public BigDecimal aplicarDescuento(Actividad actividad, BigDecimal importeBase) {
        BigDecimal descuento = actividad.getDescuentoSocioPorcentaje() == null
                ? BigDecimal.ZERO
                : actividad.getDescuentoSocioPorcentaje();
        BigDecimal factor = BigDecimal.ONE.subtract(
                descuento.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return importeBase.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /** Compatibilidad con llamadas anteriores: importe base + descuento */
    public BigDecimal calcularImporteMensual(Actividad actividad, BigDecimal importeBase) {
        return aplicarDescuento(actividad, importeBase);
    }

    /** Fianza: precio mes completo con descuento aplicado (solo extraescolares) */
    public BigDecimal calcularFianza(Actividad actividad) {
        if (!actividad.isTieneFianza() || actividad.getImporteFianza() == null) {
            return null;
        }
        return calcularImporteMensual(actividad, actividad.getImporteFianza());
    }

    public long countInscripcionesActivas(Actividad a) {
        return a.getInscripciones().stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.ACTIVA)
                .count();
    }

    public BigDecimal totalMensualActivo(Actividad a) {
        return a.getInscripciones().stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.ACTIVA)
                .map(Inscripcion::getImporteMensualCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------
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
}