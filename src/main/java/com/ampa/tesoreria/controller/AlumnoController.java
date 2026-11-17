package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.AlumnoForm;
import com.ampa.tesoreria.model.Alumno;
import com.ampa.tesoreria.service.AlumnoService;
import com.ampa.tesoreria.service.SocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/socios/{socioId}/alumnos")
@RequiredArgsConstructor
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final SocioService socioService;

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nuevo")
    public String nuevo(@PathVariable Long socioId, Model model) {
        model.addAttribute("socio", socioService.findById(socioId));
        model.addAttribute("form", alumnoService.toForm(Alumno.builder()
                .socio(socioService.findById(socioId))
                .contactosEmergencia(new java.util.ArrayList<>())
                .build()));
        model.addAttribute("editando", false);
        return "socios/alumno-form";
    }

    @PostMapping("/nuevo")
    public String crear(@PathVariable Long socioId,
                        @ModelAttribute("form") AlumnoForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            alumnoService.crear(socioId, form);
            redirectAttributes.addFlashAttribute("success", "Alumno anadido correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar: " + e.getMessage());
        }
        return "redirect:/socios/" + socioId;
    }

    // ------------------------------------------------------------
    // EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/{alumnoId}/editar")
    public String editar(@PathVariable Long socioId, @PathVariable Long alumnoId, Model model) {
        Alumno alumno = alumnoService.findById(alumnoId);
        model.addAttribute("socio", socioService.findById(socioId));
        model.addAttribute("form", alumnoService.toForm(alumno));
        model.addAttribute("editando", true);
        return "socios/alumno-form";
    }

    @PostMapping("/{alumnoId}/editar")
    public String actualizar(@PathVariable Long socioId,
                             @PathVariable Long alumnoId,
                             @ModelAttribute("form") AlumnoForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            alumnoService.actualizar(alumnoId, form);
            redirectAttributes.addFlashAttribute("success", "Alumno actualizado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar: " + e.getMessage());
        }
        return "redirect:/socios/" + socioId;
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{alumnoId}/borrar")
    public String borrar(@PathVariable Long socioId,
                         @PathVariable Long alumnoId,
                         RedirectAttributes redirectAttributes) {
        try {
            alumnoService.eliminar(alumnoId);
            redirectAttributes.addFlashAttribute("success", "Alumno eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
        }
        return "redirect:/socios/" + socioId;
    }
}