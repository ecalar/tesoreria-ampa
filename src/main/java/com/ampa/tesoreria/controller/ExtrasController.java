package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.service.ActividadService;
import com.ampa.tesoreria.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/extras")
@RequiredArgsConstructor
public class ExtrasController {

    private final ProductoService productoService;
    private final ActividadService actividadService;

    @GetMapping
    public String inicio(Model model) {
        // Contadores rápidos para las tarjetas
        model.addAttribute("numProductos", productoService.listar(null).size());
        model.addAttribute("numActividades",
                actividadService.listarPorTipo(
                        com.ampa.tesoreria.model.enums.TipoActividad.EXTRA_ACTIVIDAD, null).size());
        return "extras/index";
    }
}