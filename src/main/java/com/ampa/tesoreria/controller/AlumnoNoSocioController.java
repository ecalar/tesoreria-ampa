package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.AlumnoForm;
import com.ampa.tesoreria.service.AlumnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/alumnos")
@RequiredArgsConstructor
public class AlumnoNoSocioController {

    private final AlumnoService alumnoService;

    @GetMapping("/nuevo-no-socio")
    public String nuevo(@RequestParam(required = false) String returnTo, Model model) {
        AlumnoForm form = new AlumnoForm();
        form.setAutorizacionDesplazamientoMedico(false);

        // 4 huecos vacios para contactos de emergencia
        for (int i = 1; i <= 4; i++) {
            AlumnoForm.ContactoForm c = new AlumnoForm.ContactoForm();
            c.setOrden(i);
            form.getContactos().add(c);
        }

        model.addAttribute("form", form);
        model.addAttribute("returnTo", returnTo);
        model.addAttribute("editando", false);
        return "alumnos/alumno-no-socio-form";
    }

    @PostMapping("/nuevo-no-socio")
    public String crear(@ModelAttribute("form") AlumnoForm form,
                        @RequestParam(required = false) String returnTo,
                        RedirectAttributes redirectAttributes) {
        try {
            var alumno = alumnoService.crearNoSocio(form);
            redirectAttributes.addFlashAttribute("success",
                    "Alumno no socio creado: " + form.getNombre() + " " + form.getApellidos());
            if (returnTo != null && !returnTo.isBlank()) {
                // Añadimos el id del alumno para preseleccionarlo
                String sep = returnTo.contains("?") ? "&" : "?";
                return "redirect:" + returnTo + sep + "alumnoCreado=" + alumno.getId();
            }
            return "redirect:/alumnos";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/alumnos/nuevo-no-socio" +
                    (returnTo != null ? "?returnTo=" + returnTo : "");
        }
    }

    @GetMapping("/{id}/editar-no-socio")
    public String editar(@PathVariable Long id,
                         @RequestParam(required = false) String returnTo,
                         Model model) {
        var alumno = alumnoService.findById(id);
        model.addAttribute("form", alumnoService.toForm(alumno));
        model.addAttribute("returnTo", returnTo);
        model.addAttribute("editando", true);
        return "alumnos/alumno-no-socio-form";
    }

    @PostMapping("/{id}/editar-no-socio")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") AlumnoForm form,
                             @RequestParam(required = false) String returnTo,
                             RedirectAttributes redirectAttributes) {
        try {
            alumnoService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Alumno actualizado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/alumnos/" + id + "/editar-no-socio" +
                    (returnTo != null ? "?returnTo=" + returnTo : "");
        }
        if (returnTo != null && !returnTo.isBlank()) {
            return "redirect:" + returnTo;
        }
        return "redirect:/alumnos";
    }
}