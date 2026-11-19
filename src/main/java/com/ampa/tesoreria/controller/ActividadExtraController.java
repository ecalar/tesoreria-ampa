package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.ActividadForm;
import com.ampa.tesoreria.model.Actividad;
import com.ampa.tesoreria.model.Inscripcion;
import com.ampa.tesoreria.model.enums.EstadoInscripcion;
import com.ampa.tesoreria.model.enums.Modalidad;
import com.ampa.tesoreria.model.enums.TipoActividad;
import com.ampa.tesoreria.service.ActividadService;
import com.ampa.tesoreria.service.InscripcionService;
import com.ampa.tesoreria.util.ExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/extras/actividades")
@RequiredArgsConstructor
public class ActividadExtraController {

    private final ActividadService actividadService;
    private final InscripcionService inscripcionService;

    // ------------------------------------------------------------
    // LISTADO
    // ------------------------------------------------------------
    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        List<Actividad> actividades = actividadService.listarPorTipo(TipoActividad.EXTRA_ACTIVIDAD, q);

        BigDecimal totalMensual = BigDecimal.ZERO;
        long totalInscritos = 0;
        for (Actividad a : actividades) {
            totalMensual = totalMensual.add(actividadService.totalMensualActivo(a));
            totalInscritos += actividadService.countInscripcionesActivas(a);
        }

        model.addAttribute("actividades", actividades);
        model.addAttribute("q", q);
        model.addAttribute("totalActividades", actividades.size());
        model.addAttribute("totalInscritos", totalInscritos);
        model.addAttribute("totalMensual", totalMensual);
        model.addAttribute("esExtra", true);
        return "extras/actividades/listado";
    }

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nueva")
    public String nueva(Model model) {
        ActividadForm form = new ActividadForm();
        form.setDescuentoSocioPorcentaje(BigDecimal.ZERO);
        LocalDate hoy = LocalDate.now();
        form.setFechaInicio(hoy);
        form.setFechaFin(hoy.plusMonths(1));

        model.addAttribute("form", form);
        model.addAttribute("editando", false);
        model.addAttribute("esExtra", true);
        return "extras/actividades/form";
    }

    @PostMapping("/nueva")
    public String crear(@ModelAttribute("form") ActividadForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            Actividad a = actividadService.crear(TipoActividad.EXTRA_ACTIVIDAD, form);
            redirectAttributes.addFlashAttribute("success", "Actividad creada: " + a.getNombre());
            return "redirect:/extras/actividades/" + a.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extras/actividades/nueva";
        }
    }

    // ------------------------------------------------------------
    // EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Actividad a = actividadService.findById(id);
        model.addAttribute("form", actividadService.toForm(a));
        model.addAttribute("editando", true);
        model.addAttribute("esExtra", true);
        return "extras/actividades/form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") ActividadForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            actividadService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Actividad actualizada");
            return "redirect:/extras/actividades/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extras/actividades/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // DETALLE
    // ------------------------------------------------------------
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) EstadoInscripcion estado,
                          Model model) {
        Actividad actividad = actividadService.findById(id);
        List<Inscripcion> inscripciones = inscripcionService.listar(id, q, estado);

        BigDecimal totalPagado = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        for (Inscripcion i : inscripciones) {
            totalPagado = totalPagado.add(inscripcionService.totalPagado(i));
            totalPendiente = totalPendiente.add(inscripcionService.totalPendiente(i));
        }

        model.addAttribute("actividad", actividad);
        model.addAttribute("inscripciones", inscripciones);
        model.addAttribute("q", q);
        model.addAttribute("estado", estado);
        model.addAttribute("estados", EstadoInscripcion.values());
        model.addAttribute("totalPagado", totalPagado);
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("esExtra", true);
        return "extras/actividades/detalle";
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            actividadService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Actividad eliminada");
            return "redirect:/extras/actividades";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
            return "redirect:/extras/actividades/" + id;
        }
    }

    // ------------------------------------------------------------
    // EXPORTACIONES
    // ------------------------------------------------------------
    @GetMapping("/export/listado/{formato}")
    public void exportListado(@PathVariable String formato,
                              @RequestParam(required = false) String q,
                              HttpServletResponse response) throws IOException {
        List<Actividad> actividades = actividadService.listarPorTipo(TipoActividad.EXTRA_ACTIVIDAD, q);

        List<String> headers = List.of(
                "Nombre", "Fechas", "Dias", "Horario",
                "Precio completo (EUR)", "Precio medio (EUR)",
                "Descuento socio (%)", "Inscritos", "Total activo (EUR)");

        List<List<String>> rows = new ArrayList<>();
        for (Actividad a : actividades) {
            rows.add(List.of(
                    safe(a.getNombre()),
                    (a.getFechaInicio() != null ? a.getFechaInicio() : "") + " - " +
                            (a.getFechaFin() != null ? a.getFechaFin() : ""),
                    safe(a.getDiasSemana()),
                    safe(a.getHorario()),
                    ExportUtils.money(a.getPrecioMesCompleto()),
                    ExportUtils.money(a.getPrecioMedioMes()),
                    ExportUtils.pct(a.getDescuentoSocioPorcentaje()),
                    String.valueOf(actividadService.countInscripcionesActivas(a)),
                    ExportUtils.money(actividadService.totalMensualActivo(a))
            ));
        }
        writeResponse(response, formato, "actividades_extras_listado",
                "Listado de actividades extras", headers, rows);
    }

    // ------------------------------------------------------------
    // Helper para inyectar AlumnoService sin tocar constructor
    // ------------------------------------------------------------
    @org.springframework.beans.factory.annotation.Autowired
    private com.ampa.tesoreria.service.AlumnoService alumnoService;

    private com.ampa.tesoreria.service.AlumnoService getAlumnosService() {
        return alumnoService;
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