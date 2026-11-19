package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.PedidoProductoForm;
import com.ampa.tesoreria.model.PedidoProducto;
import com.ampa.tesoreria.model.enums.MetodoPago;
import com.ampa.tesoreria.service.AlumnoService;
import com.ampa.tesoreria.service.PedidoProductoService;
import com.ampa.tesoreria.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/extras/productos/{productoId}/pedidos")
@RequiredArgsConstructor
public class PedidoProductoController {

    private final PedidoProductoService pedidoService;
    private final ProductoService productoService;
    private final AlumnoService alumnoService;

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nuevo")
    public String nuevo(@PathVariable Long productoId,
                        @RequestParam(required = false) Long alumnoCreado,
                        Model model) {
        PedidoProductoForm form = new PedidoProductoForm();
        form.setProductoId(productoId);
        form.setMetodoPago(MetodoPago.EFECTIVO);
        form.setCantidad(1);
        if (alumnoCreado != null) {
            form.setAlumnoId(alumnoCreado);
        }

        model.addAttribute("form", form);
        model.addAttribute("producto", productoService.findById(productoId));
        model.addAttribute("alumnos", alumnoService.listarTodos());
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("editando", false);
        model.addAttribute("returnTo",
                "/extras/productos/" + productoId + "/pedidos/nuevo");
        return "extras/productos/pedido-form";
    }

    @PostMapping("/nuevo")
    public String crear(@PathVariable Long productoId,
                        @ModelAttribute("form") PedidoProductoForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            form.setProductoId(productoId);
            PedidoProducto p = pedidoService.crear(form);
            redirectAttributes.addFlashAttribute("success",
                    "Pedido creado: " + p.getAlumno().getNombreCompleto());
            return "redirect:/extras/" + productoId;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extras/" + productoId + "/pedidos/nuevo";
        }
    }

    // ------------------------------------------------------------
    // EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long productoId,
                         @PathVariable Long id,
                         Model model) {
        PedidoProducto pedido = pedidoService.findById(id);
        model.addAttribute("form", pedidoService.toForm(pedido));
        model.addAttribute("producto", productoService.findById(productoId));
        model.addAttribute("alumnos", java.util.List.of(pedido.getAlumno()));
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("editando", true);
        return "extras/productos/pedido-form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long productoId,
                             @PathVariable Long id,
                             @ModelAttribute("form") PedidoProductoForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            pedidoService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Pedido actualizado");
            return "redirect:/extras/" + productoId;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extras/" + productoId + "/pedidos/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // MARCAR PAGADO / DESMARCAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/pagar")
    public String marcarPagado(@PathVariable Long productoId,
                               @PathVariable Long id,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                               RedirectAttributes redirectAttributes) {
        try {
            pedidoService.marcarPagado(id, fecha);
            redirectAttributes.addFlashAttribute("success", "Pedido marcado como pagado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/extras/" + productoId;
    }

    @PostMapping("/{id}/desmarcar")
    public String desmarcar(@PathVariable Long productoId,
                            @PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        try {
            pedidoService.desmarcarPagado(id);
            redirectAttributes.addFlashAttribute("success", "Pedido marcado como pendiente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/extras/" + productoId;
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long productoId,
                         @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            pedidoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Pedido eliminado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
        }
        return "redirect:/extras/" + productoId;
    }
}