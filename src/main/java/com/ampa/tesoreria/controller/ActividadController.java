package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.ActividadForm;
import com.ampa.tesoreria.model.Actividad;
import com.ampa.tesoreria.model.Inscripcion;
import com.ampa.tesoreria.model.PagoMensual;
import com.ampa.tesoreria.model.enums.EstadoFianza;
import com.ampa.tesoreria.model.enums.EstadoInscripcion;
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
@RequestMapping("/extraescolares")
@RequiredArgsConstructor
public class ActividadController {

    private final ActividadService actividadService;
    private final InscripcionService inscripcionService;

    // ------------------------------------------------------------
    // LISTADO DE ACTIVIDADES
    // ------------------------------------------------------------
    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        List<Actividad> actividades = actividadService.listarExtraescolares(q);

        // Totales agregados
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
        return "extraescolares/listado";
    }

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nueva")
    public String nueva(Model model) {
        ActividadForm form = new ActividadForm();
        form.setDescuentoSocioPorcentaje(BigDecimal.ZERO);
        form.setTieneFianza(true);
        // Fechas por defecto: 1 de octubre y 31 de mayo del curso natural
        int anio = LocalDate.now().getMonthValue() >= 9
                ? LocalDate.now().getYear()
                : LocalDate.now().getYear() - 1;
        form.setFechaInicio(LocalDate.of(anio, 10, 1));
        form.setFechaFin(LocalDate.of(anio + 1, 5, 31));

        model.addAttribute("form", form);
        model.addAttribute("editando", false);
        return "extraescolares/form";
    }

    @PostMapping("/nueva")
    public String crear(@ModelAttribute("form") ActividadForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            Actividad a = actividadService.crear(form);
            redirectAttributes.addFlashAttribute("success",
                    "Actividad creada: " + a.getNombre());
            return "redirect:/extraescolares/" + a.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extraescolares/nueva";
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
        return "extraescolares/form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") ActividadForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            actividadService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Actividad actualizada");
            return "redirect:/extraescolares/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extraescolares/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // DETALLE (con inscritos)
    // ------------------------------------------------------------
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) EstadoInscripcion estado,
                          Model model) {
        Actividad actividad = actividadService.findById(id);
        List<Inscripcion> inscripciones = inscripcionService.listar(id, q, estado);

        // Totales
        BigDecimal totalPagado = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        long fianzasPendientes = 0;
        long fianzasPagadas = 0;
        long fianzasDevueltas = 0;
        long fianzasAplicadas = 0;

        for (Inscripcion i : inscripciones) {
            totalPagado = totalPagado.add(inscripcionService.totalPagado(i));
            totalPendiente = totalPendiente.add(inscripcionService.totalPendiente(i));
            if (i.getFianzaEstado() != null) {
                switch (i.getFianzaEstado()) {
                    case PENDIENTE -> fianzasPendientes++;
                    case PAGADA -> fianzasPagadas++;
                    case DEVUELTA -> fianzasDevueltas++;
                    case APLICADA -> fianzasAplicadas++;
                }
            }
        }

        model.addAttribute("actividad", actividad);
        model.addAttribute("inscripciones", inscripciones);
        model.addAttribute("q", q);
        model.addAttribute("estado", estado);
        model.addAttribute("estados", EstadoInscripcion.values());
        model.addAttribute("totalPagado", totalPagado);
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("fianzasPendientes", fianzasPendientes);
        model.addAttribute("fianzasPagadas", fianzasPagadas);
        model.addAttribute("fianzasDevueltas", fianzasDevueltas);
        model.addAttribute("fianzasAplicadas", fianzasAplicadas);
        return "extraescolares/detalle";
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            actividadService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Actividad eliminada");
            return "redirect:/extraescolares";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
            return "redirect:/extraescolares/" + id;
        }
    }

    // ------------------------------------------------------------
    // EXPORTACIONES
    // ------------------------------------------------------------

    /** Listado de actividades con sus totales agregados */
    @GetMapping("/export/listado/{formato}")
    public void exportListado(@PathVariable String formato,
                              @RequestParam(required = false) String q,
                              HttpServletResponse response) throws IOException {
        List<Actividad> actividades = actividadService.listarExtraescolares(q);

        List<String> headers = List.of(
                "Nombre", "Fechas", "Dias", "Horario",
                "Precio completo (EUR)", "Precio medio (EUR)",
                "Descuento socio (%)", "Fianza (EUR)",
                "Inscritos", "Total mensual activo (EUR)");

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
                    a.isTieneFianza() ? ExportUtils.money(a.getImporteFianza()) : "",
                    String.valueOf(actividadService.countInscripcionesActivas(a)),
                    ExportUtils.money(actividadService.totalMensualActivo(a))
            ));
        }
        writeResponse(response, formato, "extraescolares_listado",
                "Listado de actividades extraescolares", headers, rows);
    }

    /** Listado de inscritos de una actividad */
    @GetMapping("/{id}/export/inscritos/{formato}")
    public void exportInscritos(@PathVariable Long id,
                                @PathVariable String formato,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) EstadoInscripcion estado,
                                HttpServletResponse response) throws IOException {
        Actividad actividad = actividadService.findById(id);
        List<Inscripcion> inscripciones = inscripcionService.listar(id, q, estado);

        List<String> headers = List.of(
                "Alumno", "Curso", "Modalidad", "Importe mensual (EUR)",
                "Estado", "Fianza estado", "Fianza importe (EUR)",
                "Pagado (EUR)", "Pendiente (EUR)", "Observaciones");

        List<List<String>> rows = new ArrayList<>();
        for (Inscripcion i : inscripciones) {
            rows.add(List.of(
                    i.getAlumno().getNombreCompleto(),
                    safe(i.getAlumno().getCurso()),
                    i.getModalidad() != null ? i.getModalidad().name() : "",
                    ExportUtils.money(i.getImporteMensualCalculado()),
                    i.getEstado() != null ? i.getEstado().name() : "",
                    i.getFianzaEstado() != null ? i.getFianzaEstado().name() : "",
                    i.getFianzaImporte() != null ? ExportUtils.money(i.getFianzaImporte()) : "",
                    ExportUtils.money(inscripcionService.totalPagado(i)),
                    ExportUtils.money(inscripcionService.totalPendiente(i)),
                    safe(i.getObservaciones())
            ));
        }
        writeResponse(response, formato, "inscritos_" + actividad.getId(),
                "Inscritos en " + actividad.getNombre(), headers, rows);
    }

    /** Resumen de totales de una actividad */
    @GetMapping("/{id}/export/totales/{formato}")
    public void exportTotales(@PathVariable Long id,
                              @PathVariable String formato,
                              HttpServletResponse response) throws IOException {
        Actividad actividad = actividadService.findById(id);
        List<Inscripcion> inscripciones = inscripcionService.listar(id, null, null);

        BigDecimal totalPagado = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        BigDecimal totalFianzasPagadas = BigDecimal.ZERO;
        BigDecimal totalFianzasDevueltas = BigDecimal.ZERO;
        long activas = 0, bajas = 0;

        for (Inscripcion i : inscripciones) {
            totalPagado = totalPagado.add(inscripcionService.totalPagado(i));
            totalPendiente = totalPendiente.add(inscripcionService.totalPendiente(i));

            if (i.getEstado() == EstadoInscripcion.ACTIVA) activas++;
            else if (i.getEstado() == EstadoInscripcion.BAJA) bajas++;

            if (i.getFianzaEstado() == EstadoFianza.PAGADA && i.getFianzaImporte() != null) {
                totalFianzasPagadas = totalFianzasPagadas.add(i.getFianzaImporte());
            }
            if (i.getFianzaEstado() == EstadoFianza.DEVUELTA && i.getDevolucionFianza() != null) {
                totalFianzasDevueltas = totalFianzasDevueltas.add(i.getDevolucionFianza().getImporte());
            }
        }

        List<String> headers = List.of("Concepto", "Valor");
        List<List<String>> rows = List.of(
                List.of("Actividad", actividad.getNombre()),
                List.of("Periodo", actividad.getFechaInicio() + " - " + actividad.getFechaFin()),
                List.of("Inscripciones activas", String.valueOf(activas)),
                List.of("Inscripciones de baja", String.valueOf(bajas)),
                List.of("Total cobrado (EUR)", ExportUtils.money(totalPagado)),
                List.of("Total pendiente (EUR)", ExportUtils.money(totalPendiente)),
                List.of("Fianzas cobradas (EUR)", ExportUtils.money(totalFianzasPagadas)),
                List.of("Fianzas devueltas (EUR)", ExportUtils.money(totalFianzasDevueltas))
        );
        writeResponse(response, formato, "totales_" + actividad.getId(),
                "Resumen de " + actividad.getNombre(), headers, rows);
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