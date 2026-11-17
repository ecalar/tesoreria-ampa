package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.CambioPasswordForm;
import com.ampa.tesoreria.model.Usuario;
import com.ampa.tesoreria.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String ver(Model model, Authentication auth) {
        model.addAttribute("form", new CambioPasswordForm());
        model.addAttribute("username", auth.getName());
        return "perfil";
    }

    @PostMapping("/cambiar-password")
    public String cambiarPassword(@ModelAttribute("form") CambioPasswordForm form,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElseThrow();

        if (!passwordEncoder.matches(form.getPasswordActual(), usuario.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "La contrasena actual no es correcta");
            return "redirect:/perfil";
        }
        if (form.getPasswordNueva() == null || form.getPasswordNueva().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "La nueva contrasena debe tener al menos 6 caracteres");
            return "redirect:/perfil";
        }
        if (!form.getPasswordNueva().equals(form.getPasswordConfirmacion())) {
            redirectAttributes.addFlashAttribute("error", "Las contrasenas nuevas no coinciden");
            return "redirect:/perfil";
        }

        usuario.setPassword(passwordEncoder.encode(form.getPasswordNueva()));
        usuarioRepository.save(usuario);

        redirectAttributes.addFlashAttribute("success", "Contrasena actualizada correctamente");
        return "redirect:/perfil";
    }
}