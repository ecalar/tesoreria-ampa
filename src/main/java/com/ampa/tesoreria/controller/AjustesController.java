package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.AjustesForm;
import com.ampa.tesoreria.service.AjusteService;
import com.ampa.tesoreria.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Controller
@RequestMapping("/ajustes")
@RequiredArgsConstructor
public class AjustesController {

    private final AjusteService ajusteService;
    private final BackupService backupService;

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
        model.addAttribute("backups", backupService.listarNombresBackups());
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

    @PostMapping("/backup")
    public String generarBackupManual(RedirectAttributes redirectAttributes) {
        try {
            Path zip = backupService.generarBackup("manual");
            redirectAttributes.addFlashAttribute("success",
                    "Backup generado: " + zip.getFileName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo generar el backup: " + e.getMessage());
        }
        return "redirect:/ajustes";
    }

    @GetMapping("/backup/{filename}")
    public ResponseEntity<Resource> descargarBackup(@PathVariable String filename) {
        try {
            Optional<Path> encontrado = backupService.listarBackups().stream()
                    .filter(p -> p.getFileName().toString().equals(filename))
                    .findFirst();
            if (encontrado.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(encontrado.get());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}