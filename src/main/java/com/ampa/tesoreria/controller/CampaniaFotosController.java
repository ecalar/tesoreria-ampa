package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.CampaniaFotosForm;
import com.ampa.tesoreria.model.CampaniaFotos;
import com.ampa.tesoreria.service.CampaniaFotosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/fotos")
@RequiredArgsConstructor
public class CampaniaFotosController {

    private final CampaniaFotosService campaniaService;

    // ------------------------------------------------------------
    // LISTADO
    // ------------------------------------------------------------
    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        List<CampaniaFotos> campanias = campaniaService.listar(q);

        BigDecimal totalRecaudado = BigDecimal.ZERO;
        BigDecimal totalCobrado = BigDecimal.ZERO;
        BigDecimal totalDevuelto = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        long totalPedidos = 0;

        for (CampaniaFotos c : campanias) {
            totalRecaudado = totalRecaudado.add(campaniaService.totalRecaudado(c));
            totalCobrado = totalCobrado.add(campaniaService.totalCobrado(c));
            totalDevuelto = totalDevuelto.add(campaniaService.totalDevuelto(c));
            totalPendiente = totalPendiente.add(campaniaService.totalPendiente(c));
            totalPedidos += c.getPedidos().size();
        }

        model.addAttribute("campanias", campanias);
        model.addAttribute("q", q);
        model.addAttribute("totalCampanias", campanias.size());
        model.addAttribute("totalPedidos", totalPedidos);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("totalCobrado", totalCobrado);
        model.addAttribute("totalDevuelto", totalDevuelto);
        model.addAttribute("totalNeto", totalCobrado.subtract(totalDevuelto));
        model.addAttribute("totalPendiente", totalPendiente);
        return "fotos/listado";
    }

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("form", campaniaService.nuevoFormulario());
        model.addAttribute("editando", false);
        return "fotos/form";
    }

    @PostMapping("/nueva")
    public String crear(@ModelAttribute("form") CampaniaFotosForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            CampaniaFotos c = campaniaService.crear(form);
            redirectAttributes.addFlashAttribute("success", "Campaña creada: " + c.getNombre());
            return "redirect:/fotos/" + c.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fotos/nueva";
        }
    }

    // ------------------------------------------------------------
    // EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        CampaniaFotos c = campaniaService.findById(id);
        model.addAttribute("form", campaniaService.toForm(c));
        model.addAttribute("editando", true);
        return "fotos/form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") CampaniaFotosForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            campaniaService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Campaña actualizada");
            return "redirect:/fotos/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fotos/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // DETALLE
    // ------------------------------------------------------------
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        CampaniaFotos campania = campaniaService.findById(id);

        model.addAttribute("campania", campania);
        model.addAttribute("pedidos", campania.getPedidos());
        model.addAttribute("totalRecaudado", campaniaService.totalRecaudado(campania));
        model.addAttribute("totalCobrado", campaniaService.totalCobrado(campania));
        model.addAttribute("totalDevuelto", campaniaService.totalDevuelto(campania));
        model.addAttribute("totalNeto", campaniaService.totalNeto(campania));
        model.addAttribute("totalPendiente", campaniaService.totalPendiente(campania));
        return "fotos/detalle";
    }

    // ------------------------------------------------------------
    // CERRAR / REABRIR
    // ------------------------------------------------------------
    @PostMapping("/{id}/cerrar")
    public String cerrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        campaniaService.cerrar(id);
        redirectAttributes.addFlashAttribute("success", "Campaña cerrada");
        return "redirect:/fotos/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        campaniaService.reabrir(id);
        redirectAttributes.addFlashAttribute("success", "Campaña reabierta");
        return "redirect:/fotos/" + id;
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            campaniaService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Campaña eliminada");
            return "redirect:/fotos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
            return "redirect:/fotos/" + id;
        }
    }
}