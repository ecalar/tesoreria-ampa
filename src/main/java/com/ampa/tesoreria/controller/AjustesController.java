package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.AjustesForm;
import com.ampa.tesoreria.service.AjusteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/ajustes")
@RequiredArgsConstructor
public class AjustesController {

    private final AjusteService ajusteService;

    @GetMapping
    public String ver(Model model) {
        AjustesForm form = new AjustesForm();
        form.setCuotaSocio(ajusteService.getBigDecimal(AjusteService.CUOTA_SOCIO, new BigDecimal("15.00")));
        form.setDescuentoSocioGlobal(ajusteService.getBigDecimal(AjusteService.DESCUENTO_SOCIO_GLOBAL, BigDecimal.ZERO));
        form.setNombreAmpa(ajusteService.getString(AjusteService.DATOS_AMPA_NOMBRE, ""));
        form.setCifAmpa(ajusteService.getString(AjusteService.DATOS_AMPA_CIF, ""));
        form.setDireccionAmpa(ajusteService.getString(AjusteService.DATOS_AMPA_DIRECCION, ""));
        form.setTelefonoAmpa(ajusteService.getString(AjusteService.DATOS_AMPA_TELEFONO, ""));
        form.setEmailAmpa(ajusteService.getString(AjusteService.DATOS_AMPA_EMAIL, ""));
        form.setCuentaAmpa(ajusteService.getString(AjusteService.DATOS_AMPA_CUENTA, ""));
        form.setIvaActivado(ajusteService.getBoolean(AjusteService.IVA_ACTIVADO, false));
        form.setIvaPorcentaje(ajusteService.getBigDecimal(AjusteService.IVA_PORCENTAJE, new BigDecimal("21")));
        form.setSesionTimeoutMinutos(ajusteService.getInt(AjusteService.SESION_TIMEOUT_MINUTOS, 30));

        model.addAttribute("form", form);
        return "ajustes";
    }

    @PostMapping
    public String guardar(@ModelAttribute("form") AjustesForm form,
                          RedirectAttributes redirectAttributes) {
        ajusteService.set(AjusteService.CUOTA_SOCIO, form.getCuotaSocio().toPlainString());
        ajusteService.set(AjusteService.DESCUENTO_SOCIO_GLOBAL, form.getDescuentoSocioGlobal().toPlainString());
        ajusteService.set(AjusteService.DATOS_AMPA_NOMBRE, nullSafe(form.getNombreAmpa()));
        ajusteService.set(AjusteService.DATOS_AMPA_CIF, nullSafe(form.getCifAmpa()));
        ajusteService.set(AjusteService.DATOS_AMPA_DIRECCION, nullSafe(form.getDireccionAmpa()));
        ajusteService.set(AjusteService.DATOS_AMPA_TELEFONO, nullSafe(form.getTelefonoAmpa()));
        ajusteService.set(AjusteService.DATOS_AMPA_EMAIL, nullSafe(form.getEmailAmpa()));
        ajusteService.set(AjusteService.DATOS_AMPA_CUENTA, nullSafe(form.getCuentaAmpa()));
        ajusteService.set(AjusteService.IVA_ACTIVADO, String.valueOf(form.isIvaActivado()));
        ajusteService.set(AjusteService.IVA_PORCENTAJE, form.getIvaPorcentaje().toPlainString());
        ajusteService.set(AjusteService.SESION_TIMEOUT_MINUTOS, String.valueOf(form.getSesionTimeoutMinutos()));

        redirectAttributes.addFlashAttribute("success", "Ajustes guardados correctamente");
        return "redirect:/ajustes";
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}