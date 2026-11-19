package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.ProductoForm;
import com.ampa.tesoreria.model.PedidoProducto;
import com.ampa.tesoreria.model.Producto;
import com.ampa.tesoreria.service.PedidoProductoService;
import com.ampa.tesoreria.service.ProductoService;
import com.ampa.tesoreria.util.ExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/extras")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;
    private final PedidoProductoService pedidoService;

    // ------------------------------------------------------------
    // LISTADO DE PRODUCTOS
    // ------------------------------------------------------------
    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        List<Producto> productos = productoService.listar(q);

        BigDecimal totalRecaudado = BigDecimal.ZERO;
        BigDecimal totalCobrado = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        long totalUnidades = 0;

        for (Producto p : productos) {
            totalRecaudado = totalRecaudado.add(productoService.totalRecaudado(p));
            totalCobrado = totalCobrado.add(productoService.totalCobrado(p));
            totalPendiente = totalPendiente.add(productoService.totalPendiente(p));
            totalUnidades += productoService.totalUnidades(p);
        }

        model.addAttribute("productos", productos);
        model.addAttribute("q", q);
        model.addAttribute("totalProductos", productos.size());
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("totalCobrado", totalCobrado);
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("totalUnidades", totalUnidades);
        return "extras/productos/listado";
    }

    // ------------------------------------------------------------
    // ALTA
    // ------------------------------------------------------------
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        ProductoForm form = new ProductoForm();
        form.setActivo(true);
        model.addAttribute("form", form);
        model.addAttribute("editando", false);
        return "extras/productos/form";
    }

    @PostMapping("/nuevo")
    public String crear(@ModelAttribute("form") ProductoForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            Producto p = productoService.crear(form);
            redirectAttributes.addFlashAttribute("success", "Producto creado: " + p.getNombre());
            return "redirect:/extras/" + p.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extras/nuevo";
        }
    }

    // ------------------------------------------------------------
    // EDICIÓN
    // ------------------------------------------------------------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Producto p = productoService.findById(id);
        model.addAttribute("form", productoService.toForm(p));
        model.addAttribute("editando", true);
        return "extras/productos/form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("form") ProductoForm form,
                             RedirectAttributes redirectAttributes) {
        try {
            productoService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success", "Producto actualizado");
            return "redirect:/extras/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/extras/" + id + "/editar";
        }
    }

    // ------------------------------------------------------------
    // DETALLE (con pedidos)
    // ------------------------------------------------------------
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id,
                          @RequestParam(required = false) String q,
                          Model model) {
        Producto producto = productoService.findById(id);
        List<PedidoProducto> pedidos = pedidoService.listarPorProducto(id, q);

        model.addAttribute("producto", producto);
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("q", q);
        model.addAttribute("totalRecaudado", productoService.totalRecaudado(producto));
        model.addAttribute("totalCobrado", productoService.totalCobrado(producto));
        model.addAttribute("totalPendiente", productoService.totalPendiente(producto));
        model.addAttribute("totalUnidades", productoService.totalUnidades(producto));
        return "extras/productos/detalle";
    }

    // ------------------------------------------------------------
    // BORRAR
    // ------------------------------------------------------------
    @PostMapping("/{id}/borrar")
    public String borrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Producto eliminado");
            return "redirect:/extras";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo borrar: " + e.getMessage());
            return "redirect:/extras/" + id;
        }
    }

    // ------------------------------------------------------------
    // EXPORTACIONES
    // ------------------------------------------------------------
    @GetMapping("/export/listado/{formato}")
    public void exportListado(@PathVariable String formato,
                              @RequestParam(required = false) String q,
                              HttpServletResponse response) throws IOException {
        List<Producto> productos = productoService.listar(q);

        List<String> headers = List.of(
                "Producto", "Descripcion", "Precio unitario (EUR)",
                "Activo", "Pedidos", "Unidades", "Cobrado (EUR)", "Pendiente (EUR)");

        List<List<String>> rows = new ArrayList<>();
        for (Producto p : productos) {
            rows.add(List.of(
                    safe(p.getNombre()),
                    safe(p.getDescripcion()),
                    ExportUtils.money(p.getPrecioUnitario()),
                    p.isActivo() ? "SI" : "NO",
                    String.valueOf(p.getPedidos().size()),
                    String.valueOf(productoService.totalUnidades(p)),
                    ExportUtils.money(productoService.totalCobrado(p)),
                    ExportUtils.money(productoService.totalPendiente(p))
            ));
        }
        writeResponse(response, formato, "productos_listado",
                "Listado de productos", headers, rows);
    }

    @GetMapping("/{id}/export/pedidos/{formato}")
    public void exportPedidos(@PathVariable Long id,
                              @PathVariable String formato,
                              @RequestParam(required = false) String q,
                              HttpServletResponse response) throws IOException {
        Producto producto = productoService.findById(id);
        List<PedidoProducto> pedidos = pedidoService.listarPorProducto(id, q);

        List<String> headers = List.of(
                "Alumno", "Curso", "Talla", "Cantidad",
                "Precio unit. (EUR)", "Subtotal (EUR)",
                "Pagado", "Fecha pedido", "Observaciones");

        List<List<String>> rows = new ArrayList<>();
        for (PedidoProducto pp : pedidos) {
            rows.add(List.of(
                    pp.getAlumno().getNombreCompleto(),
                    safe(pp.getAlumno().getCurso()),
                    safe(pp.getTalla()),
                    String.valueOf(pp.getCantidad()),
                    ExportUtils.money(pp.getPrecioUnitarioAplicado()),
                    ExportUtils.money(pp.getSubtotal()),
                    pp.isPagado() ? "SI" : "NO",
                    pp.getFechaPedido() != null ? pp.getFechaPedido().toString() : "",
                    safe(pp.getObservaciones())
            ));
        }
        writeResponse(response, formato, "producto_" + producto.getId() + "_pedidos",
                "Pedidos de " + producto.getNombre(), headers, rows);
    }

    @GetMapping("/{id}/export/totales/{formato}")
    public void exportTotales(@PathVariable Long id,
                              @PathVariable String formato,
                              HttpServletResponse response) throws IOException {
        Producto producto = productoService.findById(id);

        List<String> headers = List.of("Concepto", "Valor");
        List<List<String>> rows = List.of(
                List.of("Producto", producto.getNombre()),
                List.of("Precio unitario (EUR)", ExportUtils.money(producto.getPrecioUnitario())),
                List.of("Pedidos", String.valueOf(producto.getPedidos().size())),
                List.of("Unidades totales", String.valueOf(productoService.totalUnidades(producto))),
                List.of("Total cobrado (EUR)", ExportUtils.money(productoService.totalCobrado(producto))),
                List.of("Total pendiente (EUR)", ExportUtils.money(productoService.totalPendiente(producto))),
                List.of("Total facturado (EUR)", ExportUtils.money(productoService.totalRecaudado(producto)))
        );
        writeResponse(response, formato, "producto_" + producto.getId() + "_totales",
                "Resumen de " + producto.getNombre(), headers, rows);
    }

    // ------------------------------------------------------------
    private void writeResponse(HttpServletResponse response, String formato, String filename,
                               String titulo, List<String> headers, List<List<String>> rows)
            throws IOException {
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
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "." + extension + "\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}