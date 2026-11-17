package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.service.CursoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("cursos", cursoService.findAll());
        return "cursos";
    }

    @PostMapping("/crear")
    public String crear(@RequestParam String nombre,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                        RedirectAttributes redirectAttributes) {
        try {
            cursoService.crearCurso(nombre, fechaInicio, fechaFin);
            redirectAttributes.addFlashAttribute("success", "Curso creado correctamente: " + nombre);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cursos";
    }

    @PostMapping("/cerrar/{id}")
    public String cerrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            cursoService.cerrarCurso(id);
            redirectAttributes.addFlashAttribute("success", "Curso cerrado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo cerrar el curso: " + e.getMessage());
        }
        return "redirect:/cursos";
    }
}