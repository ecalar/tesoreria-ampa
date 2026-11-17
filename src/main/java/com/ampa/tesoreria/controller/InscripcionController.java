package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.InscripcionForm;
import com.ampa.tesoreria.model.Alumno;
import com.ampa.tesoreria.model.Inscripcion;
import com.ampa.tesoreria.model.enums.MetodoPago;
import com.ampa.tesoreria.model.enums.Modalidad;
import com.ampa.tesoreria.repository.AlumnoRepository;
import com.ampa.tesoreria.service.ActividadService;
import com.ampa.tesoreria.service.AlumnoService;
import com.ampa.tesoreria.service.InscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/extraescolares/{actividadId}/inscripciones")
@RequiredArgsConstructor
public class InscripcionController {

    private final InscripcionService inscripcionService;
    private final ActividadService actividadService;
    private final AlumnoService alumnoService;
    private final AlumnoRepository alumnoRepository;

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nueva")
    public String nueva(@PathVariable Long actividadId,
                        @RequestParam(required = false) String alumnoCreado,
                        Model model) {
        InscripcionForm form = new InscripcionForm();
        form.setActividadId(actividadId);
        form.setModalidad(Modalidad.COMPLETO);

        model.addAttribute("form", form);
        model.addAttribute("actividad", actividadService.findById(actividadId));
        model.addAttribute("alumnos", alumnoService.listarTodos());
        model.addAttribute("modalidades", List.of(Modalidad.COMPLETO, Modalidad.MEDIO));
        model.addAttribute("editando", false);
        model.addAttribute("returnTo", "/extraescolares/" + actividadId + "/inscripciones/nueva");
        return "extraescolares/inscripcion-form";
    }

    @PostMapping("/nueva")
    public String crear(@PathVariable Long actividadId,
                        @ModelAttribute("form") InscripcionForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            form.setActividadId(actividadId);
            Inscripcion i = inscripcionService.inscribir(form);
            redirectAttributes.addFlashAttribute("success",
                    "Alumno inscrito: " + i.getAlumno().getNombreCompleto());
            return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + i.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extraescolares/" + actividadId + "/inscripciones/nueva";
        }
    }

    // ------------------------------------------------------------
    // DETALLE
    // ------------------------------------------------------------
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long actividadId, @PathVariable Long id, Model model) {
        Inscripcion i = inscripcionService.findById(id);
        model.addAttribute("actividad", actividadService.findById(actividadId));
        model.addAttribute("inscripcion", i);
        model.addAttribute("pagos", inscripcionService.pagosDe(id));
        model.addAttribute("totalPagado", inscripcionService.totalPagado(i));
        model.addAttribute("totalPendiente", inscripcionService.totalPendiente(i));
        model.addAttribute("metodosPago", MetodoPago.values());
        return "extraescolares/inscripcion-detalle";
    }

    // ------------------------------------------------------------
    // EDICIÓN (modalidad / observaciones)
    // ------------------------------------------------------------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long actividadId, @PathVariable Long id, Model model) {
        Inscripcion i = inscripcionService.findById(id);
        InscripcionForm form = new InscripcionForm();
        form.setId(i.getId());
        form.setActividadId(actividadId);
        form.setAlumnoId(i.getAlumno().getId());
        form.setModalidad(i.getModalidad());
        form.setObservaciones(i.getObservaciones());
        form.setPermisoImagen(i.getPermisoImagen());

        model.addAttribute("form", form);
        model.addAttribute("actividad", actividadService.findById(actividadId));
        model.addAttribute("alumnos", List.of(i.getAlumno()));
        model.addAttribute("modalidades", List.of(Modalidad.COMPLETO, Modalidad.MEDIO));
        model.addAttribute("editando", true);
        return "extraescolares/inscripcion-form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long actividadId,
                             @PathVariable Long id,
                             @ModelAttribute("form") InscripcionForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            inscripcionService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Inscripcion actualizada");
            return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // BAJA / REACTIVAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/baja")
    public String darDeBaja(@PathVariable Long actividadId,
                            @PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        inscripcionService.darDeBaja(id);
        redirectAttributes.addFlashAttribute("success", "Alumno dado de baja");
        return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + id;
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long actividadId,
                            @PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        inscripcionService.reactivar(id);
        redirectAttributes.addFlashAttribute("success", "Inscripcion reactivada");
        return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + id;
    }

    // ------------------------------------------------------------
    // PAGOS MENSUALES
    // ------------------------------------------------------------
    @PostMapping("/{id}/pagos/{pagoId}/marcar")
    public String marcarPago(@PathVariable Long actividadId,
                             @PathVariable Long id,
                             @PathVariable Long pagoId,
                             @RequestParam MetodoPago metodo,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                             RedirectAttributes redirectAttributes) {
        try {
            inscripcionService.marcarMesPagado(id, pagoId, metodo, fecha);
            redirectAttributes.addFlashAttribute("success", "Mes marcado como pagado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + id;
    }

    @PostMapping("/{id}/pagos/{pagoId}/desmarcar")
    public String desmarcarPago(@PathVariable Long actividadId,
                                @PathVariable Long id,
                                @PathVariable Long pagoId,
                                RedirectAttributes redirectAttributes) {
        try {
            inscripcionService.desmarcarMesPagado(id, pagoId);
            redirectAttributes.addFlashAttribute("success", "Pago desmarcado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + id;
    }

    // ------------------------------------------------------------
    // FIANZA
    // ------------------------------------------------------------
    @PostMapping("/{id}/fianza/pagar")
    public String pagarFianza(@PathVariable Long actividadId,
                              @PathVariable Long id,
                              @RequestParam MetodoPago metodo,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                              RedirectAttributes redirectAttributes) {
        try {
            inscripcionService.marcarFianzaPagada(id, metodo, fecha);
            redirectAttributes.addFlashAttribute("success", "Fianza marcada como pagada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + id;
    }

    @PostMapping("/{id}/fianza/devolver")
    public String devolverFianza(@PathVariable Long actividadId,
                                 @PathVariable Long id,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                 @RequestParam(required = false) BigDecimal importe,
                                 @RequestParam MetodoPago metodo,
                                 @RequestParam(required = false) String observaciones,
                                 RedirectAttributes redirectAttributes) {
        try {
            inscripcionService.devolverFianza(id, fecha, importe, metodo, observaciones);
            redirectAttributes.addFlashAttribute("success", "Fianza devuelta");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + id;
    }

    @PostMapping("/{id}/fianza/aplicar")
    public String aplicarFianza(@PathVariable Long actividadId,
                                @PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            inscripcionService.aplicarFianza(id);
            redirectAttributes.addFlashAttribute("success",
                    "Fianza aplicada al ultimo mes del periodo");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/extraescolares/" + actividadId + "/inscripciones/" + id;
    }
}