package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.SocioForm;
import com.ampa.tesoreria.model.Socio;
import com.ampa.tesoreria.model.enums.MetodoPago;
import com.ampa.tesoreria.service.AlumnoService;
import com.ampa.tesoreria.service.CursoService;
import com.ampa.tesoreria.service.SocioService;
import com.ampa.tesoreria.util.ExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/socios")
@RequiredArgsConstructor
public class SociosController {

    private final SocioService socioService;
    private final AlumnoService alumnoService;
    private final CursoService cursoService;

    // ------------------------------------------------------------
    // LISTADO
    // ------------------------------------------------------------
    @GetMapping
    public String listar(@RequestParam(required = false) Long cursoId,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) MetodoPago metodoPago,
                         Model model) {
        Long idCurso = (cursoId != null) ? cursoId : cursoService.getCursoActivo().getId();
        List<Socio> socios = socioService.listar(idCurso, q, metodoPago);

        model.addAttribute("socios", socios);
        model.addAttribute("cursos", cursoService.findAll());
        model.addAttribute("cursoId", idCurso);
        model.addAttribute("q", q);
        model.addAttribute("metodoPago", metodoPago);
        model.addAttribute("metodos", MetodoPago.values());

        // Totales
        model.addAttribute("total", socioService.totalRecaudado(socios));
        model.addAttribute("numSocios", socios.size());
        model.addAttribute("totalEfectivo", socioService.totalPorMetodo(socios, MetodoPago.EFECTIVO));
        model.addAttribute("totalTransferencia", socioService.totalPorMetodo(socios, MetodoPago.TRANSFERENCIA));
        model.addAttribute("numEfectivo", socioService.countPorMetodo(socios, MetodoPago.EFECTIVO));
        model.addAttribute("numTransferencia", socioService.countPorMetodo(socios, MetodoPago.TRANSFERENCIA));

        return "socios/listado";
    }

    // ------------------------------------------------------------
    // ALTA / EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        SocioForm form = new SocioForm();
        form.setMetodoPago(MetodoPago.EFECTIVO);
        form.setPermisoImagen(true);
        model.addAttribute("form", form);
        model.addAttribute("editando", false);
        model.addAttribute("metodos", MetodoPago.values());
        return "socios/form";
    }

    @PostMapping("/nuevo")
    public String crear(@ModelAttribute("form") SocioForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            Socio socio = socioService.crear(form);
            redirectAttributes.addFlashAttribute("success",
                    "Socio creado correctamente: " + socio.getNombre() + " " + socio.getApellidos());
            return "redirect:/socios/" + socio.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/socios/nuevo";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Socio socio = socioService.findById(id);
        SocioForm form = new SocioForm();
        form.setId(socio.getId());
        form.setNombre(socio.getNombre());
        form.setApellidos(socio.getApellidos());
        form.setDni(socio.getDni());
        form.setDireccion(socio.getDireccion());
        form.setTelefono(socio.getTelefono());
        form.setEmail(socio.getEmail());
        form.setPermisoImagen(socio.isPermisoImagen());
        form.setNumeroRecibo(socio.getNumeroRecibo());
        form.setMetodoPago(socio.getMetodoPago());

        model.addAttribute("form", form);
        model.addAttribute("editando", true);
        model.addAttribute("metodos", MetodoPago.values());
        return "socios/form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") SocioForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            socioService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Socio actualizado correctamente");
            return "redirect:/socios/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/socios/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // DETALLE (con alumnos)
    // ------------------------------------------------------------
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Socio socio = socioService.findById(id);
        model.addAttribute("socio", socio);
        model.addAttribute("alumnos", alumnoService.findBySocio(id));
        return "socios/detalle";
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            socioService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Socio eliminado correctamente");
            return "redirect:/socios";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
            return "redirect:/socios";
        }
    }

    // ------------------------------------------------------------
    // EXPORTACIONES DEL LISTADO
    // ------------------------------------------------------------
    @GetMapping("/export/listado/{formato}")
    public void exportListado(@PathVariable String formato,
                              @RequestParam(required = false) Long cursoId,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) MetodoPago metodoPago,
                              HttpServletResponse response) throws IOException {
        Long idCurso = (cursoId != null) ? cursoId : cursoService.getCursoActivo().getId();
        List<Socio> socios = socioService.listar(idCurso, q, metodoPago);

        List<String> headers = List.of("Nombre", "Apellidos", "DNI", "Email", "Telefono",
                "Metodo pago", "N. recibo", "Cuota (EUR)", "Fecha alta", "Alumnos");

        List<List<String>> rows = new ArrayList<>();
        for (Socio s : socios) {
            rows.add(List.of(
                    safe(s.getNombre()),
                    safe(s.getApellidos()),
                    safe(s.getDni()),
                    safe(s.getEmail()),
                    safe(s.getTelefono()),
                    s.getMetodoPago().name(),
                    safe(s.getNumeroRecibo()),
                    ExportUtils.money(s.getCuotaImporte()),
                    s.getFechaAlta() != null ? s.getFechaAlta().toString() : "",
                    String.valueOf(s.getAlumnos().size())
            ));
        }

        String filename = "socios_listado";
        writeResponse(response, formato, filename, "Listado de socios",
                headers, rows, MediaType.APPLICATION_OCTET_STREAM);
    }

    // ------------------------------------------------------------
    // EXPORTACIONES DEL TOTAL
    // ------------------------------------------------------------
    @GetMapping("/export/totales/{formato}")
    public void exportTotales(@PathVariable String formato,
                              @RequestParam(required = false) Long cursoId,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) MetodoPago metodoPago,
                              HttpServletResponse response) throws IOException {
        Long idCurso = (cursoId != null) ? cursoId : cursoService.getCursoActivo().getId();
        List<Socio> socios = socioService.listar(idCurso, q, metodoPago);

        List<String> headers = List.of("Concepto", "Valor");
        List<List<String>> rows = List.of(
                List.of("Curso", cursoService.findById(idCurso).getNombre()),
                List.of("Numero de socios", String.valueOf(socios.size())),
                List.of("Socios por EFECTIVO", String.valueOf(socioService.countPorMetodo(socios, MetodoPago.EFECTIVO))),
                List.of("Socios por TRANSFERENCIA", String.valueOf(socioService.countPorMetodo(socios, MetodoPago.TRANSFERENCIA))),
                List.of("Importe EFECTIVO (EUR)", ExportUtils.money(socioService.totalPorMetodo(socios, MetodoPago.EFECTIVO))),
                List.of("Importe TRANSFERENCIA (EUR)", ExportUtils.money(socioService.totalPorMetodo(socios, MetodoPago.TRANSFERENCIA))),
                List.of("TOTAL RECAUDADO (EUR)", ExportUtils.money(socioService.totalRecaudado(socios)))
        );

        String filename = "socios_totales";
        writeResponse(response, formato, filename, "Resumen de ingresos por socios",
                headers, rows, MediaType.APPLICATION_OCTET_STREAM);
    }

    // ------------------------------------------------------------
    private void writeResponse(HttpServletResponse response, String formato, String filename,
                               String titulo, List<String> headers, List<List<String>> rows,
                               MediaType fallbackType) throws IOException {
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
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "." + extension + "\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}