package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.service.CursoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final CursoService cursoService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String index(Model model) {
        try {
            model.addAttribute("cursoActivo", cursoService.getCursoActivo());
        } catch (IllegalStateException e) {
            model.addAttribute("cursoActivo", null);
            model.addAttribute("sinCurso", true);
        }
        return "index";
    }
}