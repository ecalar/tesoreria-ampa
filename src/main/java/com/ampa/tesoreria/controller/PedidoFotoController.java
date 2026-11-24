package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.PedidoFotoForm;
import com.ampa.tesoreria.model.PedidoFoto;
import com.ampa.tesoreria.model.enums.MetodoPago;
import com.ampa.tesoreria.service.AlumnoService;
import com.ampa.tesoreria.service.CampaniaFotosService;
import com.ampa.tesoreria.service.PedidoFotoService;
import com.ampa.tesoreria.service.SocioService;
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
@RequestMapping("/fotos/{campaniaId}/pedidos")
@RequiredArgsConstructor
public class PedidoFotoController {

    private final PedidoFotoService pedidoService;
    private final CampaniaFotosService campaniaService;
    private final AlumnoService alumnoService;
    private final SocioService socioService;

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nuevo")
    public String nuevo(@PathVariable Long campaniaId,
                        @RequestParam(required = false) Long alumnoCreado,
                        Model model) {
        PedidoFotoForm form = new PedidoFotoForm();
        form.setCampaniaId(campaniaId);
        form.setMetodoPago(MetodoPago.EFECTIVO);
        form.setNumLotes(0);
        form.setNumIndividuales(0);

        // Sin alumno preseleccionado (se elige tras seleccionar socio)
        // Si viene alumnoCreado, significa que se acaba de crear un no socio,
        // así que sí lo preseleccionamos.
        if (alumnoCreado != null) {
            form.setAlumnoId(alumnoCreado);
        }

        // Sin grupos ni hermanos iniciales (se añaden dinámicamente en JS)
        form.setGrupos(new java.util.ArrayList<>());
        form.setHermanos(new java.util.ArrayList<>());

        model.addAttribute("form", form);
        model.addAttribute("campania", campaniaService.findById(campaniaId));
        model.addAttribute("socios", socioService.listarTodos());
        model.addAttribute("alumnos", alumnoService.listarTodos()); // para el caso "no socio"
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("editando", false);
        model.addAttribute("returnTo",
                "/fotos/" + campaniaId + "/pedidos/nuevo");
        return "fotos/pedido-form";
    }

    @PostMapping("/nuevo")
    public String crear(@PathVariable Long campaniaId,
                        @ModelAttribute("form") PedidoFotoForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            form.setCampaniaId(campaniaId);
            PedidoFoto p = pedidoService.crear(form);
            redirectAttributes.addFlashAttribute("success",
                    "Pedido creado: " + p.getAlumno().getNombreCompleto());
            return "redirect:/fotos/" + campaniaId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fotos/" + campaniaId + "/pedidos/nuevo";
        }
    }

    // ------------------------------------------------------------
    // EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long campaniaId, @PathVariable Long id, Model model) {
        PedidoFoto pedido = pedidoService.findById(id);
        model.addAttribute("form", pedidoService.toForm(pedido));
        model.addAttribute("campania", campaniaService.findById(campaniaId));
        model.addAttribute("alumnos", List.of(pedido.getAlumno()));
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("editando", true);
        model.addAttribute("returnTo",
                "/fotos/" + campaniaId + "/pedidos/" + id + "/editar");
        model.addAttribute("socios", socioService.listarTodos());
        return "fotos/pedido-form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long campaniaId,
                             @PathVariable Long id,
                             @ModelAttribute("form") PedidoFotoForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            pedidoService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Pedido actualizado");
            return "redirect:/fotos/" + campaniaId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fotos/" + campaniaId + "/pedidos/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // MARCAR PAGADO / DESMARCAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/pagar")
    public String marcarPagado(@PathVariable Long campaniaId,
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
        return "redirect:/fotos/" + campaniaId;
    }

    @PostMapping("/{id}/desmarcar")
    public String desmarcar(@PathVariable Long campaniaId,
                            @PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        try {
            pedidoService.desmarcarPagado(id);
            redirectAttributes.addFlashAttribute("success", "Pedido marcado como pendiente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/fotos/" + campaniaId;
    }

    // ------------------------------------------------------------
    // DEVOLUCIÓN
    // ------------------------------------------------------------
    @PostMapping("/{id}/devolucion")
    public String registrarDevolucion(@PathVariable Long campaniaId,
                                      @PathVariable Long id,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                      @RequestParam BigDecimal importe,
                                      @RequestParam MetodoPago metodo,
                                      @RequestParam(required = false) String observaciones,
                                      RedirectAttributes redirectAttributes) {
        try {
            pedidoService.registrarDevolucion(id, fecha, importe, metodo, observaciones);
            redirectAttributes.addFlashAttribute("success", "Devolucion registrada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/fotos/" + campaniaId;
    }

    @PostMapping("/{id}/devolucion/eliminar")
    public String eliminarDevolucion(@PathVariable Long campaniaId,
                                     @PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        try {
            pedidoService.eliminarDevolucion(id);
            redirectAttributes.addFlashAttribute("success", "Devolucion eliminada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/fotos/" + campaniaId;
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long campaniaId,
                         @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            pedidoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Pedido eliminado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
        }
        return "redirect:/fotos/" + campaniaId;
    }
}