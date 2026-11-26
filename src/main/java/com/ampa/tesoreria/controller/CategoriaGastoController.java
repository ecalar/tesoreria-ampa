package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.CategoriaGastoForm;
import com.ampa.tesoreria.model.CategoriaGasto;
import com.ampa.tesoreria.service.CategoriaGastoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/ajustes/categorias-gastos")
@RequiredArgsConstructor
public class CategoriaGastoController {

    private final CategoriaGastoService categoriaService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "ajustes/categorias-gastos";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        CategoriaGastoForm form = new CategoriaGastoForm();
        form.setActiva(true);
        model.addAttribute("form", form);
        model.addAttribute("editando", false);
        return "ajustes/categoria-gasto-form";
    }

    @PostMapping("/nueva")
    public String crear(@ModelAttribute("form") CategoriaGastoForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            categoriaService.crear(form);
            redirectAttributes.addFlashAttribute("success", "Categoría creada");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ajustes/categorias-gastos/nueva";
        }
        return "redirect:/ajustes/categorias-gastos";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        CategoriaGasto c = categoriaService.findById(id);
        model.addAttribute("form", categoriaService.toForm(c));
        model.addAttribute("editando", true);
        return "ajustes/categoria-gasto-form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") CategoriaGastoForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            categoriaService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Categoría actualizada");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ajustes/categorias-gastos/" + id + "/editar";
        }
        return "redirect:/ajustes/categorias-gastos";
    }

    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Categoría eliminada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ajustes/categorias-gastos";
    }
}