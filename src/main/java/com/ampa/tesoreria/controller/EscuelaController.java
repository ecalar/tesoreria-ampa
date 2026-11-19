package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.ActividadForm;
import com.ampa.tesoreria.model.Actividad;
import com.ampa.tesoreria.model.Inscripcion;
import com.ampa.tesoreria.model.enums.EstadoInscripcion;
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
@RequestMapping("/escuelas")
@RequiredArgsConstructor
public class EscuelaController {

    private final ActividadService actividadService;
    private final InscripcionService inscripcionService;

    // ------------------------------------------------------------
    // LISTADO
    // ------------------------------------------------------------
    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        List<Actividad> escuelas = actividadService.listarEscuelas(q);

        BigDecimal totalMensual = BigDecimal.ZERO;
        long totalInscritos = 0;
        for (Actividad a : escuelas) {
            totalMensual = totalMensual.add(actividadService.totalMensualActivo(a));
            totalInscritos += actividadService.countInscripcionesActivas(a);
        }

        model.addAttribute("escuelas", escuelas);
        model.addAttribute("q", q);
        model.addAttribute("totalEscuelas", escuelas.size());
        model.addAttribute("totalInscritos", totalInscritos);
        model.addAttribute("totalMensual", totalMensual);
        return "escuelas/listado";
    }

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nueva")
    public String nueva(Model model) {
        ActividadForm form = new ActividadForm();
        form.setDescuentoSocioPorcentaje(BigDecimal.ZERO);
        int anio = LocalDate.now().getMonthValue() >= 9
                ? LocalDate.now().getYear()
                : LocalDate.now().getYear() - 1;
        form.setFechaInicio(LocalDate.of(anio, 12, 20));
        form.setFechaFin(LocalDate.of(anio + 1, 1, 7));

        model.addAttribute("form", form);
        model.addAttribute("editando", false);
        return "escuelas/form";
    }

    @PostMapping("/nueva")
    public String crear(@ModelAttribute("form") ActividadForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            Actividad a = actividadService.crear(TipoActividad.ESCUELA, form);
            redirectAttributes.addFlashAttribute("success", "Escuela creada: " + a.getNombre());
            return "redirect:/escuelas/" + a.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/escuelas/nueva";
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
        return "escuelas/form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") ActividadForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            actividadService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Escuela actualizada");
            return "redirect:/escuelas/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/escuelas/" + id + "/editar";
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
        Actividad escuela = actividadService.findById(id);
        List<Inscripcion> inscripciones = inscripcionService.listar(id, q, estado);

        BigDecimal totalPagado = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        for (Inscripcion i : inscripciones) {
            totalPagado = totalPagado.add(inscripcionService.totalPagado(i));
            totalPendiente = totalPendiente.add(inscripcionService.totalPendiente(i));
        }

        model.addAttribute("escuela", escuela);
        model.addAttribute("actividad", escuela); // alias para reusar plantillas
        model.addAttribute("inscripciones", inscripciones);
        model.addAttribute("q", q);
        model.addAttribute("estado", estado);
        model.addAttribute("estados", EstadoInscripcion.values());
        model.addAttribute("totalPagado", totalPagado);
        model.addAttribute("totalPendiente", totalPendiente);
        return "escuelas/detalle";
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            actividadService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Escuela eliminada");
            return "redirect:/escuelas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
            return "redirect:/escuelas/" + id;
        }
    }

    // ------------------------------------------------------------
    // EXPORTACIONES
    // ------------------------------------------------------------
    @GetMapping("/export/listado/{formato}")
    public void exportListado(@PathVariable String formato,
                              @RequestParam(required = false) String q,
                              HttpServletResponse response) throws IOException {
        List<Actividad> escuelas = actividadService.listarEscuelas(q);

        List<String> headers = List.of(
                "Nombre", "Fechas", "Dias", "Horario",
                "Precio completo (EUR)", "Precio medio (EUR)", "Precio/dia (EUR)",
                "Descuento socio (%)", "Inscritos", "Total activo (EUR)");

        List<List<String>> rows = new ArrayList<>();
        for (Actividad a : escuelas) {
            rows.add(List.of(
                    safe(a.getNombre()),
                    (a.getFechaInicio() != null ? a.getFechaInicio() : "") + " - " +
                            (a.getFechaFin() != null ? a.getFechaFin() : ""),
                    safe(a.getDiasSemana()),
                    safe(a.getHorario()),
                    ExportUtils.money(a.getPrecioMesCompleto()),
                    ExportUtils.money(a.getPrecioMedioMes()),
                    ExportUtils.money(a.getPrecioPorDia()),
                    ExportUtils.pct(a.getDescuentoSocioPorcentaje()),
                    String.valueOf(actividadService.countInscripcionesActivas(a)),
                    ExportUtils.money(actividadService.totalMensualActivo(a))
            ));
        }
        writeResponse(response, formato, "escuelas_listado",
                "Listado de escuelas", headers, rows);
    }

    @GetMapping("/{id}/export/inscritos/{formato}")
    public void exportInscritos(@PathVariable Long id,
                                @PathVariable String formato,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) EstadoInscripcion estado,
                                HttpServletResponse response) throws IOException {
        Actividad escuela = actividadService.findById(id);
        List<Inscripcion> inscripciones = inscripcionService.listar(id, q, estado);

        List<String> headers = List.of(
                "Alumno", "Curso", "Modalidad", "Dias", "Importe (EUR)",
                "Estado", "Pagado (EUR)", "Pendiente (EUR)", "Observaciones");

        List<List<String>> rows = new ArrayList<>();
        for (Inscripcion i : inscripciones) {
            rows.add(List.of(
                    i.getAlumno().getNombreCompleto(),
                    safe(i.getAlumno().getCurso()),
                    i.getModalidad() != null ? i.getModalidad().name() : "",
                    i.getDiasAsistidos() != null ? String.valueOf(i.getDiasAsistidos()) : "",
                    ExportUtils.money(i.getImporteMensualCalculado()),
                    i.getEstado() != null ? i.getEstado().name() : "",
                    ExportUtils.money(inscripcionService.totalPagado(i)),
                    ExportUtils.money(inscripcionService.totalPendiente(i)),
                    safe(i.getObservaciones())
            ));
        }
        writeResponse(response, formato, "escuela_" + escuela.getId() + "_inscritos",
                "Inscritos en " + escuela.getNombre(), headers, rows);
    }

    @GetMapping("/{id}/export/totales/{formato}")
    public void exportTotales(@PathVariable Long id,
                              @PathVariable String formato,
                              HttpServletResponse response) throws IOException {
        Actividad escuela = actividadService.findById(id);
        List<Inscripcion> inscripciones = inscripcionService.listar(id, null, null);

        BigDecimal totalPagado = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        long activas = 0, bajas = 0;

        for (Inscripcion i : inscripciones) {
            totalPagado = totalPagado.add(inscripcionService.totalPagado(i));
            totalPendiente = totalPendiente.add(inscripcionService.totalPendiente(i));
            if (i.getEstado() == EstadoInscripcion.ACTIVA) activas++;
            else if (i.getEstado() == EstadoInscripcion.BAJA) bajas++;
        }

        List<String> headers = List.of("Concepto", "Valor");
        List<List<String>> rows = List.of(
                List.of("Escuela", escuela.getNombre()),
                List.of("Periodo", escuela.getFechaInicio() + " - " + escuela.getFechaFin()),
                List.of("Inscripciones activas", String.valueOf(activas)),
                List.of("Inscripciones de baja", String.valueOf(bajas)),
                List.of("Total cobrado (EUR)", ExportUtils.money(totalPagado)),
                List.of("Total pendiente (EUR)", ExportUtils.money(totalPendiente))
        );
        writeResponse(response, formato, "escuela_" + escuela.getId() + "_totales",
                "Resumen de " + escuela.getNombre(), headers, rows);
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