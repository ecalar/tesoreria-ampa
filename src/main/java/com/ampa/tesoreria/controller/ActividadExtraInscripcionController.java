package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.InscripcionForm;
import com.ampa.tesoreria.model.Inscripcion;
import com.ampa.tesoreria.model.enums.MetodoPago;
import com.ampa.tesoreria.model.enums.Modalidad;
import com.ampa.tesoreria.service.ActividadService;
import com.ampa.tesoreria.service.AlumnoService;
import com.ampa.tesoreria.service.InscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/extras/actividades/{actividadId}/inscripciones")
@RequiredArgsConstructor
public class ActividadExtraInscripcionController {

    private final InscripcionService inscripcionService;
    private final ActividadService actividadService;
    private final AlumnoService alumnoService;

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nueva")
    public String nueva(@PathVariable Long actividadId,
                        @RequestParam(required = false) Long alumnoCreado,
                        Model model) {
        InscripcionForm form = new InscripcionForm();
        form.setActividadId(actividadId);
        form.setModalidad(Modalidad.COMPLETO);
        if (alumnoCreado != null) {
            form.setAlumnoId(alumnoCreado);
        }

        var actividad = actividadService.findById(actividadId);
        List<Modalidad> modalidades = (actividad.getPrecioPorDia() != null)
                ? List.of(Modalidad.COMPLETO, Modalidad.MEDIO, Modalidad.POR_DIAS)
                : List.of(Modalidad.COMPLETO, Modalidad.MEDIO);

        model.addAttribute("form", form);
        model.addAttribute("actividad", actividad);
        model.addAttribute("alumnos", alumnoService.listarTodos());
        model.addAttribute("modalidades", modalidadesDisponibles(actividad));
        model.addAttribute("editando", false);
        model.addAttribute("returnTo",
                "/extras/actividades/" + actividadId + "/inscripciones/nueva");
        return "extras/actividades/inscripcion-form";
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
            return "redirect:/extras/actividades/" + actividadId + "/inscripciones/" + i.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extras/actividades/" + actividadId + "/inscripciones/nueva";
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
        return "extras/actividades/inscripcion-detalle";
    }

    // ------------------------------------------------------------
    // EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long actividadId, @PathVariable Long id, Model model) {
        Inscripcion i = inscripcionService.findById(id);
        InscripcionForm form = new InscripcionForm();
        form.setId(i.getId());
        form.setActividadId(actividadId);
        form.setAlumnoId(i.getAlumno().getId());
        form.setModalidad(i.getModalidad());
        form.setDiasAsistidos(i.getDiasAsistidos());
        form.setDiasSemanaSeleccionados(i.getDiasSemanaSeleccionados());
        form.setObservaciones(i.getObservaciones());
        form.setPermisoImagen(i.getPermisoImagen());

        var actividad = actividadService.findById(actividadId);
        List<Modalidad> modalidades = (actividad.getPrecioPorDia() != null)
                ? List.of(Modalidad.COMPLETO, Modalidad.MEDIO, Modalidad.POR_DIAS)
                : List.of(Modalidad.COMPLETO, Modalidad.MEDIO);

        model.addAttribute("form", form);
        model.addAttribute("actividad", actividad);
        model.addAttribute("alumnos", List.of(i.getAlumno()));
        model.addAttribute("modalidades", modalidadesDisponibles(actividad));
        model.addAttribute("editando", true);
        model.addAttribute("returnTo",
                "/extras/actividades/" + actividadId + "/inscripciones/" + id + "/editar");
        String textoModalidad = switch (modalidades.size()) {
            case 1 -> "Esta actividad solo admite pago completo.";
            case 2 -> "COMPLETO = periodo completo · POR_DIAS = solo los dias elegidos";
            default -> "COMPLETO = periodo completo · MEDIO = mitad · POR_DIAS = solo los dias elegidos";
        };
        model.addAttribute("textoModalidad", textoModalidad);
        return "extras/actividades/inscripcion-form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long actividadId,
                             @PathVariable Long id,
                             @ModelAttribute("form") InscripcionForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            inscripcionService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Inscripcion actualizada");
            return "redirect:/extras/actividades/" + actividadId + "/inscripciones/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extras/actividades/" + actividadId + "/inscripciones/" + id + "/editar";
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
        return "redirect:/extras/actividades/" + actividadId + "/inscripciones/" + id;
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long actividadId,
                            @PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        inscripcionService.reactivar(id);
        redirectAttributes.addFlashAttribute("success", "Inscripcion reactivada");
        return "redirect:/extras/actividades/" + actividadId + "/inscripciones/" + id;
    }

    // ------------------------------------------------------------
    // PAGOS
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
        return "redirect:/extras/actividades/" + actividadId + "/inscripciones/" + id;
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
        return "redirect:/extras/actividades/" + actividadId + "/inscripciones/" + id;
    }

    private List<Modalidad> modalidadesDisponibles(com.ampa.tesoreria.model.Actividad actividad) {
        List<Modalidad> modalidades = new java.util.ArrayList<>();
        modalidades.add(Modalidad.COMPLETO); // siempre disponible
        if (actividad.getPrecioMedioMes() != null) {
            modalidades.add(Modalidad.MEDIO);
        }
        if (actividad.getPrecioPorDia() != null) {
            modalidades.add(Modalidad.POR_DIAS);
        }
        return modalidades;
    }
}