package com.ampa.tesoreria.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PlaceholderController {

    @GetMapping({"/fotos", "/gastos", "/contabilidad"})
    public String placeholder(jakarta.servlet.http.HttpServletRequest request, Model model) {
        model.addAttribute("seccion", request.getRequestURI().substring(1));
        return "placeholder";
    }
}