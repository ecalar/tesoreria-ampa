package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.GastoForm;
import com.ampa.tesoreria.model.Gasto;
import com.ampa.tesoreria.model.enums.MetodoPago;
import com.ampa.tesoreria.service.CategoriaGastoService;
import com.ampa.tesoreria.service.GastoService;
import com.ampa.tesoreria.util.ExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/gastos")
@RequiredArgsConstructor
public class GastoController {

    private final GastoService gastoService;
    private final CategoriaGastoService categoriaService;

    // ------------------------------------------------------------
    // LISTADO
    // ------------------------------------------------------------
    @GetMapping
    public String listar(@RequestParam(required = false) Long categoriaId,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         @RequestParam(required = false) String q,
                         Model model) {
        List<Gasto> gastos = gastoService.listar(categoriaId, desde, hasta, q);

        BigDecimal total = gastoService.totalGastos(gastos);

        // Desglose por categoría
        java.util.Map<String, BigDecimal> porCategoria = new java.util.LinkedHashMap<>();
        for (Gasto g : gastos) {
            String cat = g.getCategoria() != null ? g.getCategoria().getNombre() : "Sin categoría";
            porCategoria.merge(cat, g.getImporteTotal(), BigDecimal::add);
        }

        // Desglose por método de pago
        BigDecimal totalEfectivo = gastos.stream()
                .filter(g -> g.getMetodoPago() == MetodoPago.EFECTIVO)
                .map(Gasto::getImporteTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTransferencia = gastos.stream()
                .filter(g -> g.getMetodoPago() == MetodoPago.TRANSFERENCIA)
                .map(Gasto::getImporteTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("gastos", gastos);
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("q", q);
        model.addAttribute("total", total);
        model.addAttribute("porCategoria", porCategoria);
        model.addAttribute("totalEfectivo", totalEfectivo);
        model.addAttribute("totalTransferencia", totalTransferencia);
        model.addAttribute("numGastos", gastos.size());
        return "gastos/listado";
    }

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        GastoForm form = new GastoForm();
        form.setFecha(LocalDate.now());
        form.setMetodoPago(MetodoPago.EFECTIVO);
        form.setIvaPorcentaje(BigDecimal.ZERO);
        model.addAttribute("form", form);
        model.addAttribute("categorias", categoriaService.listarActivas());
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("editando", false);
        return "gastos/form";
    }

    @PostMapping("/nuevo")
    public String crear(@ModelAttribute("form") GastoForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            gastoService.crear(form);
            redirectAttributes.addFlashAttribute("success", "Gasto creado");
            return "redirect:/gastos";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/gastos/nuevo";
        }
    }

    // ------------------------------------------------------------
    // EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Gasto g = gastoService.findById(id);
        model.addAttribute("form", gastoService.toForm(g));
        model.addAttribute("categorias", categoriaService.listarActivas());
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("editando", true);
        return "gastos/form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") GastoForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            gastoService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Gasto actualizado");
            return "redirect:/gastos";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/gastos/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            gastoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Gasto eliminado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
        }
        return "redirect:/gastos";
    }

    // ------------------------------------------------------------
    // DESCARGAR FACTURA
    // ------------------------------------------------------------
    @GetMapping("/{id}/factura")
    public ResponseEntity<Resource> descargarFactura(@PathVariable Long id) {
        try {
            Gasto g = gastoService.findById(id);
            Path path = gastoService.getRutaFactura(id);
            Resource resource = new FileSystemResource(path);
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            String nombreDescarga = g.getNombreFacturaOriginal() != null
                    ? g.getNombreFacturaOriginal() : "factura";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + nombreDescarga + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ------------------------------------------------------------
    // EXPORTACIONES
    // ------------------------------------------------------------
    @GetMapping("/export/{formato}")
    public void export(@PathVariable String formato,
                       @RequestParam(required = false) Long categoriaId,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                       @RequestParam(required = false) String q,
                       HttpServletResponse response) throws IOException {
        List<Gasto> gastos = gastoService.listar(categoriaId, desde, hasta, q);

        List<String> headers = List.of(
                "Fecha", "Concepto", "Categoría", "Proveedor",
                "Importe base", "IVA (%)", "Importe total",
                "Método", "Factura", "Observaciones");

        List<List<String>> rows = new ArrayList<>();
        for (Gasto g : gastos) {
            rows.add(List.of(
                    g.getFecha() != null ? g.getFecha().toString() : "",
                    safe(g.getConcepto()),
                    g.getCategoria() != null ? g.getCategoria().getNombre() : "",
                    safe(g.getProveedor()),
                    ExportUtils.money(g.getImporteBase()),
                    ExportUtils.pct(g.getIvaPorcentaje()),
                    ExportUtils.money(g.getImporteTotal()),
                    g.getMetodoPago() != null ? g.getMetodoPago().name() : "",
                    g.getRutaFactura() != null ? "SÍ" : "NO",
                    safe(g.getObservaciones())
            ));
        }

        // Fila de totales
        BigDecimal total = gastoService.totalGastos(gastos);
        rows.add(List.of("TOTAL", "", "", "", "", "", ExportUtils.money(total), "", "", ""));

        writeResponse(response, formato, "gastos_listado",
                "Listado de gastos", headers, rows);
    }

    // ------------------------------------------------------------
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