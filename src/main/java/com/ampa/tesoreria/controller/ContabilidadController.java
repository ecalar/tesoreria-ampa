package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.MovimientoCaja;
import com.ampa.tesoreria.dto.ResumenApartado;
import com.ampa.tesoreria.dto.ResumenMensual;
import com.ampa.tesoreria.service.ContabilidadService;
import com.ampa.tesoreria.util.ExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/contabilidad")
@RequiredArgsConstructor
public class ContabilidadController {

    private final ContabilidadService contabilidadService;

    // ============================================================
    // LIBRO DE CAJA
    // ============================================================
    @GetMapping
    public String libroCaja(@RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            Model model) {
        List<MovimientoCaja> movimientos = contabilidadService.libroDeCaja(desde, hasta);

        model.addAttribute("movimientos", movimientos);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("totalIngresos", contabilidadService.totalIngresos(movimientos));
        model.addAttribute("totalGastos", contabilidadService.totalGastos(movimientos));
        model.addAttribute("saldoFinal", contabilidadService.saldoFinal(movimientos));
        model.addAttribute("vista", "libro");
        return "contabilidad/index";
    }

    // ============================================================
    // RESUMEN MENSUAL
    // ============================================================
    @GetMapping("/mensual")
    public String resumenMensual(@RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                 Model model) {
        List<ResumenMensual> resumen = contabilidadService.resumenMensual(desde, hasta);
        BigDecimal totalIngresos = resumen.stream().map(ResumenMensual::getIngresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGastos = resumen.stream().map(ResumenMensual::getGastos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("resumen", resumen);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("totalIngresos", totalIngresos);
        model.addAttribute("totalGastos", totalGastos);
        model.addAttribute("saldoFinal", totalIngresos.subtract(totalGastos));
        model.addAttribute("vista", "mensual");
        return "contabilidad/index";
    }

    // ============================================================
    // RESUMEN POR APARTADO
    // ============================================================
    @GetMapping("/apartados")
    public String resumenApartados(@RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   Model model) {
        List<ResumenApartado> resumen = contabilidadService.resumenPorApartado(desde, hasta);
        BigDecimal totalIngresos = resumen.stream().map(ResumenApartado::getIngresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDevoluciones = resumen.stream().map(ResumenApartado::getDevoluciones)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("resumen", resumen);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("totalIngresos", totalIngresos);
        model.addAttribute("totalDevoluciones", totalDevoluciones);
        model.addAttribute("saldoFinal", totalIngresos.subtract(totalDevoluciones));
        model.addAttribute("vista", "apartados");
        return "contabilidad/index";
    }

    // ============================================================
    // EXPORTACIONES: LIBRO DE CAJA
    // ============================================================
    @GetMapping("/export/libro/{formato}")
    public void exportLibro(@PathVariable String formato,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            HttpServletResponse response) throws IOException {
        List<MovimientoCaja> movimientos = contabilidadService.libroDeCaja(desde, hasta);

        List<String> headers = List.of("Fecha", "Categoría", "Concepto", "Detalle",
                "Ingreso", "Gasto", "Saldo acumulado");
        List<List<String>> rows = new ArrayList<>();
        for (MovimientoCaja m : movimientos) {
            rows.add(List.of(
                    m.getFecha() != null ? m.getFecha().toString() : "",
                    safe(m.getCategoria()),
                    safe(m.getConcepto()),
                    safe(m.getDetalle()),
                    m.getIngreso() != null ? ExportUtils.money(m.getIngreso()) : "",
                    m.getGasto() != null ? ExportUtils.money(m.getGasto()) : "",
                    m.getSaldoAcumulado() != null ? ExportUtils.money(m.getSaldoAcumulado()) : ""
            ));
        }
        rows.add(List.of("TOTAL", "", "", "",
                ExportUtils.money(contabilidadService.totalIngresos(movimientos)),
                ExportUtils.money(contabilidadService.totalGastos(movimientos)),
                ExportUtils.money(contabilidadService.saldoFinal(movimientos))));

        writeResponse(response, formato, "libro_caja", "Libro de caja", headers, rows);
    }

    // ============================================================
    // EXPORTACIONES: RESUMEN MENSUAL
    // ============================================================
    @GetMapping("/export/mensual/{formato}")
    public void exportMensual(@PathVariable String formato,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                              HttpServletResponse response) throws IOException {
        List<ResumenMensual> resumen = contabilidadService.resumenMensual(desde, hasta);

        List<String> headers = List.of("Mes", "Ingresos", "Gastos", "Saldo");
        List<List<String>> rows = new ArrayList<>();
        for (ResumenMensual r : resumen) {
            rows.add(List.of(
                    r.getMes().toString(),
                    ExportUtils.money(r.getIngresos()),
                    ExportUtils.money(r.getGastos()),
                    ExportUtils.money(r.getSaldo())
            ));
        }
        BigDecimal ti = resumen.stream().map(ResumenMensual::getIngresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tg = resumen.stream().map(ResumenMensual::getGastos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        rows.add(List.of("TOTAL", ExportUtils.money(ti), ExportUtils.money(tg),
                ExportUtils.money(ti.subtract(tg))));

        writeResponse(response, formato, "resumen_mensual", "Resumen mensual", headers, rows);
    }

    // ============================================================
    // EXPORTACIONES: RESUMEN POR APARTADO
    // ============================================================
    @GetMapping("/export/apartados/{formato}")
    public void exportApartados(@PathVariable String formato,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                HttpServletResponse response) throws IOException {
        List<ResumenApartado> resumen = contabilidadService.resumenPorApartado(desde, hasta);

        List<String> headers = List.of("Apartado", "Ingresos", "Devoluciones", "Neto");
        List<List<String>> rows = new ArrayList<>();
        for (ResumenApartado r : resumen) {
            rows.add(List.of(
                    r.getApartado(),
                    ExportUtils.money(r.getIngresos()),
                    ExportUtils.money(r.getDevoluciones()),
                    ExportUtils.money(r.getNeto())
            ));
        }
        BigDecimal ti = resumen.stream().map(ResumenApartado::getIngresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal td = resumen.stream().map(ResumenApartado::getDevoluciones)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        rows.add(List.of("TOTAL", ExportUtils.money(ti), ExportUtils.money(td),
                ExportUtils.money(ti.subtract(td))));

        writeResponse(response, formato, "resumen_apartados", "Resumen por apartado", headers, rows);
    }

    // ============================================================
    private void writeResponse(HttpServletResponse response, String formato, String filename,
                               String titulo, List<String> headers, List<List<String>> rows)
            throws IOException {
        byte[] data;
        String contentType;
        String extension;

        switch (formato.toLowerCase()) {
            case "csv" -> {
                data = ExportUtils.toCsv(headers, rows);
                contentType = "text/csv; charset=UTF-8";
                extension = "csv";
            }
            case "xlsx" -> {
                data = ExportUtils.toExcel(titulo, headers, rows);
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = "xlsx";
            }
            case "pdf" -> {
                data = ExportUtils.toPdf(titulo, headers, rows);
                contentType = "application/pdf";
                extension = "pdf";
            }
            default -> throw new IllegalArgumentException("Formato no soportado: " + formato);
        }

        response.setContentType(contentType);
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "." + extension + "\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}