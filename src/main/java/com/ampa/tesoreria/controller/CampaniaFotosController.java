package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.CampaniaFotosForm;
import com.ampa.tesoreria.model.CampaniaFotos;
import com.ampa.tesoreria.service.CampaniaFotosService;
import com.ampa.tesoreria.service.PedidoFotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ampa.tesoreria.model.PedidoFoto;
import com.ampa.tesoreria.util.ExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/fotos")
@RequiredArgsConstructor
public class CampaniaFotosController {

    private final CampaniaFotosService campaniaService;
    private final PedidoFotoService pedidoService;


    // ------------------------------------------------------------
    // LISTADO
    // ------------------------------------------------------------
    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        List<CampaniaFotos> campanias = campaniaService.listar(q);

        BigDecimal totalRecaudado = BigDecimal.ZERO;
        BigDecimal totalCobrado = BigDecimal.ZERO;
        BigDecimal totalDevuelto = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        long totalPedidos = 0;

        for (CampaniaFotos c : campanias) {
            totalRecaudado = totalRecaudado.add(campaniaService.totalRecaudado(c));
            totalCobrado = totalCobrado.add(campaniaService.totalCobrado(c));
            totalDevuelto = totalDevuelto.add(campaniaService.totalDevuelto(c));
            totalPendiente = totalPendiente.add(campaniaService.totalPendiente(c));
            totalPedidos += c.getPedidos().size();
        }

        model.addAttribute("campanias", campanias);
        model.addAttribute("q", q);
        model.addAttribute("totalCampanias", campanias.size());
        model.addAttribute("totalPedidos", totalPedidos);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("totalCobrado", totalCobrado);
        model.addAttribute("totalDevuelto", totalDevuelto);
        model.addAttribute("totalNeto", totalCobrado.subtract(totalDevuelto));
        model.addAttribute("totalPendiente", totalPendiente);
        return "fotos/listado";
    }

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("form", campaniaService.nuevoFormulario());
        model.addAttribute("editando", false);
        return "fotos/form";
    }

    @PostMapping("/nueva")
    public String crear(@ModelAttribute("form") CampaniaFotosForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            CampaniaFotos c = campaniaService.crear(form);
            redirectAttributes.addFlashAttribute("success", "Campaña creada: " + c.getNombre());
            return "redirect:/fotos/" + c.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fotos/nueva";
        }
    }

    // ------------------------------------------------------------
    // EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        CampaniaFotos c = campaniaService.findById(id);
        model.addAttribute("form", campaniaService.toForm(c));
        model.addAttribute("editando", true);
        return "fotos/form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") CampaniaFotosForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            campaniaService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Campaña actualizada");
            return "redirect:/fotos/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fotos/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // DETALLE
    // ------------------------------------------------------------
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        CampaniaFotos campania = campaniaService.findById(id);

        // Cargamos los pedidos con @EntityGraph (alumno + grupos + hermanos)
        List<com.ampa.tesoreria.model.PedidoFoto> pedidos =
                pedidoService.listarPorCampania(id, null, null);

        model.addAttribute("campania", campania);
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("totalRecaudado", campaniaService.totalRecaudado(campania));
        model.addAttribute("totalCobrado", campaniaService.totalCobrado(campania));
        model.addAttribute("totalDevuelto", campaniaService.totalDevuelto(campania));
        model.addAttribute("totalNeto", campaniaService.totalNeto(campania));
        model.addAttribute("totalPendiente", campaniaService.totalPendiente(campania));
        return "fotos/detalle";
    }

    // ------------------------------------------------------------
    // CERRAR / REABRIR
    // ------------------------------------------------------------
    @PostMapping("/{id}/cerrar")
    public String cerrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        campaniaService.cerrar(id);
        redirectAttributes.addFlashAttribute("success", "Campaña cerrada");
        return "redirect:/fotos/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        campaniaService.reabrir(id);
        redirectAttributes.addFlashAttribute("success", "Campaña reabierta");
        return "redirect:/fotos/" + id;
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            campaniaService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Campaña eliminada");
            return "redirect:/fotos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
            return "redirect:/fotos/" + id;
        }
    }

    // ------------------------------------------------------------
// EXPORTACIONES
// ------------------------------------------------------------

    /**
     * Listado ORGANIZATIVO para el fotografo.
     * Alumno, curso, hermanos, lotes, grupales desglosadas, individuales, observaciones.
     * SIN importes.
     */
    @GetMapping("/{id}/export/organizativo/{formato}")
    public void exportOrganizativo(@PathVariable Long id,
                                   @PathVariable String formato,
                                   HttpServletResponse response) throws IOException {
        CampaniaFotos campania = campaniaService.findById(id);
        List<PedidoFoto> pedidos = pedidoService.listarPorCampania(id, null, null);

        List<String> headers = List.of(
                "Alumno", "Curso", "Hermanos",
                "Lotes", "Grupales (desglose)", "Individuales", "Observaciones");

        List<List<String>> rows = new ArrayList<>();
        for (PedidoFoto p : pedidos) {
            // Hermanos: nombre (curso), nombre (curso)...
            String hermanos = "-";
            if (p.getHermanos() != null && !p.getHermanos().isEmpty()) {
                hermanos = p.getHermanos().stream()
                        .map(h -> h.getNombre() + (h.getCurso() != null ? " (" + h.getCurso() + ")" : ""))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("-");
            }

            // Grupales: "1ºB x1, 3ºA x2"
            String grupales = "-";
            if (p.getGrupos() != null && !p.getGrupos().isEmpty()) {
                grupales = p.getGrupos().stream()
                        .map(g -> g.getCurso() + " x" + g.getCantidad())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("-");
            }

            rows.add(List.of(
                    p.getAlumno().getNombreCompleto(),
                    safe(p.getCursoAlumno()),
                    hermanos,
                    String.valueOf(p.getNumLotes() != null ? p.getNumLotes() : 0),
                    grupales,
                    String.valueOf(p.getNumIndividuales() != null ? p.getNumIndividuales() : 0),
                    safe(p.getObservaciones())
            ));
        }

        writeResponse(response, formato,
                "fotos_" + campania.getId() + "_organizativo",
                "Fotos - " + campania.getNombre() + " (organizativo para fotografo)",
                headers, rows);
    }

    /**
     * Listado de INGRESOS para tesoreria.
     * Alumno, curso, lotes, total grupales, individuales, subtotal, descuento, total,
     * metodo, pagado, devolucion, observaciones.
     */
    @GetMapping("/{id}/export/ingresos/{formato}")
    public void exportIngresos(@PathVariable Long id,
                               @PathVariable String formato,
                               HttpServletResponse response) throws IOException {
        CampaniaFotos campania = campaniaService.findById(id);
        List<PedidoFoto> pedidos = pedidoService.listarPorCampania(id, null, null);

        List<String> headers = List.of(
                "Alumno", "Curso",
                "Lotes", "P. lote (EUR)", "Total lotes (EUR)",
                "Grupales", "P. grupal (EUR)", "Total grupales (EUR)",
                "Individuales", "P. individual (EUR)", "Total individuales (EUR)",
                "Subtotal (EUR)", "Dto. (%)", "TOTAL (EUR)",
                "Metodo", "Pagado", "Devuelto (EUR)", "Observaciones");

        List<List<String>> rows = new ArrayList<>();
        for (PedidoFoto p : pedidos) {
            int numLotes = p.getNumLotes() != null ? p.getNumLotes() : 0;
            int numIndividuales = p.getNumIndividuales() != null ? p.getNumIndividuales() : 0;
            int totalGrupales = p.getTotalGrupales();

            rows.add(List.of(
                    p.getAlumno().getNombreCompleto(),
                    safe(p.getCursoAlumno()),
                    String.valueOf(numLotes),
                    ExportUtils.money(p.getPrecioLoteAplicado()),
                    ExportUtils.money(p.getPrecioLoteAplicado().multiply(java.math.BigDecimal.valueOf(numLotes))),
                    String.valueOf(totalGrupales),
                    ExportUtils.money(p.getPrecioGrupalAplicado()),
                    ExportUtils.money(p.getPrecioGrupalAplicado().multiply(java.math.BigDecimal.valueOf(totalGrupales))),
                    String.valueOf(numIndividuales),
                    ExportUtils.money(p.getPrecioIndividualAplicado()),
                    ExportUtils.money(p.getPrecioIndividualAplicado().multiply(java.math.BigDecimal.valueOf(numIndividuales))),
                    ExportUtils.money(p.getSubtotal()),
                    ExportUtils.pct(p.getDescuentoAplicado()),
                    ExportUtils.money(p.getTotal()),
                    p.getMetodoPago() != null ? p.getMetodoPago().name() : "",
                    p.isPagado() ? "SI" : "NO",
                    p.getDevolucion() != null ? ExportUtils.money(p.getDevolucion().getImporte()) : "",
                    safe(p.getObservaciones())
            ));
        }

        writeResponse(response, formato,
                "fotos_" + campania.getId() + "_ingresos",
                "Fotos - " + campania.getNombre() + " (ingresos)",
                headers, rows);
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