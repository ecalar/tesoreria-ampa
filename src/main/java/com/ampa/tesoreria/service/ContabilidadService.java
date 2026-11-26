package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.MovimientoCaja;
import com.ampa.tesoreria.dto.ResumenApartado;
import com.ampa.tesoreria.dto.ResumenMensual;
import com.ampa.tesoreria.model.*;
import com.ampa.tesoreria.model.enums.EstadoFianza;
import com.ampa.tesoreria.model.enums.EstadoInscripcion;
import com.ampa.tesoreria.model.enums.TipoActividad;
import com.ampa.tesoreria.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContabilidadService {

    private final CursoService cursoService;
    private final SocioRepository socioRepository;
    private final ActividadRepository actividadRepository;
    private final InscripcionRepository inscripcionRepository;
    private final GastoRepository gastoRepository;
    private final ProductoRepository productoRepository;
    private final PedidoProductoRepository pedidoProductoRepository;
    private final CampaniaFotosRepository campaniaFotosRepository;
    private final PedidoFotoRepository pedidoFotoRepository;

    // ============================================================
    // LIBRO DE CAJA
    // ============================================================
    public List<MovimientoCaja> libroDeCaja(LocalDate desde, LocalDate hasta) {
        Long cursoId = cursoService.getCursoActivo().getId();
        List<MovimientoCaja> movimientos = new ArrayList<>();

        // --- INGRESOS ---

        // 1. Socios (cuotas)
        for (Socio s : socioRepository.findByCursoId(cursoId)) {
            movimientos.add(new MovimientoCaja(
                    s.getFechaAlta(),
                    "Cuota socio",
                    "Socios",
                    s.getNombre() + " " + s.getApellidos(),
                    s.getCuotaImporte(),
                    null
            ));
        }

        // 2. Pagos mensuales de actividades (extraescolares, escuelas, actividades extras)
        List<Actividad> actividades = actividadRepository.findByCursoId(cursoId);
        for (Actividad a : actividades) {
            for (Inscripcion i : a.getInscripciones()) {
                if (i.getEstado() != EstadoInscripcion.ACTIVA) continue;

                String cat = switch (a.getTipo()) {
                    case EXTRAESCOLAR -> "Extraescolares";
                    case ESCUELA -> "Escuelas";
                    case EXTRA_ACTIVIDAD -> "Extras - Actividades";
                };

                // Pagos mensuales pagados
                for (PagoMensual p : i.getPagosMensuales()) {
                    if (p.isPagado() && p.getFechaPago() != null) {
                        movimientos.add(new MovimientoCaja(
                                p.getFechaPago(),
                                "Pago " + p.getMes(),
                                cat,
                                i.getAlumno().getNombreCompleto() + " - " + a.getNombre(),
                                p.getImporte(),
                                null
                        ));
                    }
                }

                // Fianza pagada (solo extraescolares)
                if (a.getTipo() == TipoActividad.EXTRAESCOLAR
                        && i.getFianzaEstado() == EstadoFianza.PAGADA
                        && i.getFianzaFechaPago() != null) {
                    movimientos.add(new MovimientoCaja(
                            i.getFianzaFechaPago(),
                            "Fianza",
                            "Extraescolares",
                            i.getAlumno().getNombreCompleto() + " - " + a.getNombre(),
                            i.getFianzaImporte(),
                            null
                    ));
                }

                // Devolución de fianza (es un gasto)
                if (i.getDevolucionFianza() != null) {
                    movimientos.add(new MovimientoCaja(
                            i.getDevolucionFianza().getFecha(),
                            "Devolución fianza",
                            "Extraescolares",
                            i.getAlumno().getNombreCompleto() + " - " + a.getNombre(),
                            null,
                            i.getDevolucionFianza().getImporte()
                    ));
                }

                // Devolución de inscripción (actividades extras)
                if (i.getDevolucionInscripcion() != null) {
                    movimientos.add(new MovimientoCaja(
                            i.getDevolucionInscripcion().getFecha(),
                            "Devolución inscripción",
                            cat,
                            i.getAlumno().getNombreCompleto() + " - " + a.getNombre(),
                            null,
                            i.getDevolucionInscripcion().getImporte()
                    ));
                }
            }
        }

        // 3. Pedidos de producto pagados
        for (Producto p : productoRepository.findByCursoId(cursoId)) {
            for (PedidoProducto pp : p.getPedidos()) {
                if (pp.isPagado() && pp.getFechaPago() != null) {
                    movimientos.add(new MovimientoCaja(
                            pp.getFechaPago(),
                            "Pedido producto",
                            "Extras - Productos",
                            p.getNombre() + " - " + pp.getAlumno().getNombreCompleto(),
                            pp.getSubtotal(),
                            null
                    ));
                }
                if (pp.getDevolucion() != null) {
                    movimientos.add(new MovimientoCaja(
                            pp.getDevolucion().getFecha(),
                            "Devolución producto",
                            "Extras - Productos",
                            p.getNombre() + " - " + pp.getAlumno().getNombreCompleto(),
                            null,
                            pp.getDevolucion().getImporte()
                    ));
                }
            }
        }

        // 4. Pedidos de fotos pagados
        for (CampaniaFotos c : campaniaFotosRepository.findByCursoId(cursoId)) {
            for (PedidoFoto pf : c.getPedidos()) {
                if (pf.isPagado() && pf.getFechaPago() != null) {
                    movimientos.add(new MovimientoCaja(
                            pf.getFechaPago(),
                            "Pedido fotos",
                            "Fotos",
                            pf.getAlumno().getNombreCompleto() + " - " + c.getNombre(),
                            pf.getTotal(),
                            null
                    ));
                }
                if (pf.getDevolucion() != null) {
                    movimientos.add(new MovimientoCaja(
                            pf.getDevolucion().getFecha(),
                            "Devolución fotos",
                            "Fotos",
                            pf.getAlumno().getNombreCompleto() + " - " + c.getNombre(),
                            null,
                            pf.getDevolucion().getImporte()
                    ));
                }
            }
        }

        // --- GASTOS ---
        for (Gasto g : gastoRepository.findByCursoIdOrderByFechaDesc(cursoId)) {
            movimientos.add(new MovimientoCaja(
                    g.getFecha(),
                    g.getConcepto(),
                    "Gastos",
                    g.getProveedor() != null ? g.getProveedor() : "",
                    null,
                    g.getImporteTotal()
            ));
        }

        // Filtrar por fechas
        if (desde != null) {
            movimientos = movimientos.stream()
                    .filter(m -> !m.getFecha().isBefore(desde)).toList();
        }
        if (hasta != null) {
            movimientos = movimientos.stream()
                    .filter(m -> !m.getFecha().isAfter(hasta)).toList();
        }

        // Ordenar por fecha
        List<MovimientoCaja> ordenados = new ArrayList<>(movimientos);
        ordenados.sort(Comparator.comparing(MovimientoCaja::getFecha));

        // Calcular saldo acumulado
        BigDecimal saldo = BigDecimal.ZERO;
        for (MovimientoCaja m : ordenados) {
            if (m.getIngreso() != null) saldo = saldo.add(m.getIngreso());
            if (m.getGasto() != null) saldo = saldo.subtract(m.getGasto());
            m.setSaldoAcumulado(saldo);
        }
        return ordenados;
    }

    // ============================================================
    // RESUMEN MENSUAL
    // ============================================================
    public List<ResumenMensual> resumenMensual(LocalDate desde, LocalDate hasta) {
        List<MovimientoCaja> movimientos = libroDeCaja(desde, hasta);

        Map<YearMonth, BigDecimal[]> datos = new TreeMap<>();
        for (MovimientoCaja m : movimientos) {
            YearMonth mes = YearMonth.from(m.getFecha());
            BigDecimal[] v = datos.computeIfAbsent(mes, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (m.getIngreso() != null) v[0] = v[0].add(m.getIngreso());
            if (m.getGasto() != null) v[1] = v[1].add(m.getGasto());
        }

        return datos.entrySet().stream()
                .map(e -> new ResumenMensual(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    // ============================================================
    // RESUMEN POR APARTADO
    // ============================================================
    public List<ResumenApartado> resumenPorApartado(LocalDate desde, LocalDate hasta) {
        List<MovimientoCaja> movimientos = libroDeCaja(desde, hasta);

        Map<String, BigDecimal[]> datos = new TreeMap<>();
        for (MovimientoCaja m : movimientos) {
            BigDecimal[] v = datos.computeIfAbsent(m.getCategoria(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (m.getIngreso() != null) v[0] = v[0].add(m.getIngreso());
            if (m.getGasto() != null) v[1] = v[1].add(m.getGasto());
        }

        return datos.entrySet().stream()
                .map(e -> new ResumenApartado(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    // ============================================================
    // TOTALES
    // ============================================================
    public BigDecimal totalIngresos(List<MovimientoCaja> movimientos) {
        return movimientos.stream()
                .filter(m -> m.getIngreso() != null)
                .map(MovimientoCaja::getIngreso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalGastos(List<MovimientoCaja> movimientos) {
        return movimientos.stream()
                .filter(m -> m.getGasto() != null)
                .map(MovimientoCaja::getGasto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal saldoFinal(List<MovimientoCaja> movimientos) {
        return totalIngresos(movimientos).subtract(totalGastos(movimientos));
    }
}