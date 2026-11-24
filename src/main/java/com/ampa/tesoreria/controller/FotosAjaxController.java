package com.ampa.tesoreria.controller;

import com.ampa.tesoreria.dto.AlumnoSimpleDTO;
import com.ampa.tesoreria.model.Alumno;
import com.ampa.tesoreria.model.Socio;
import com.ampa.tesoreria.service.AlumnoService;
import com.ampa.tesoreria.service.SocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fotos/ajax")
@RequiredArgsConstructor
public class FotosAjaxController {

    private final SocioService socioService;
    private final AlumnoService alumnoService;

    /** Alumnos de un socio */
    @GetMapping("/socios/{socioId}/alumnos")
    public List<AlumnoSimpleDTO> alumnosDeSocio(@PathVariable Long socioId) {
        return alumnoService.findBySocio(socioId).stream()
                .map(this::toDTO)
                .toList();
    }

    /** Hermanos de un alumno (excluye al propio alumno) */
    @GetMapping("/alumnos/{alumnoId}/hermanos")
    public List<AlumnoSimpleDTO> hermanosDeAlumno(@PathVariable Long alumnoId) {
        Alumno alumno = alumnoService.findById(alumnoId);
        if (alumno.getSocio() == null) {
            return List.of();
        }
        return alumnoService.findBySocio(alumno.getSocio().getId()).stream()
                .filter(a -> !a.getId().equals(alumnoId))
                .map(this::toDTO)
                .toList();
    }

    /** Datos básicos de un alumno (para autocompletar curso al elegir) */
    @GetMapping("/alumnos/{alumnoId}")
    public AlumnoSimpleDTO alumno(@PathVariable Long alumnoId) {
        return toDTO(alumnoService.findById(alumnoId));
    }

    private AlumnoSimpleDTO toDTO(Alumno a) {
        AlumnoSimpleDTO dto = new AlumnoSimpleDTO();
        dto.setId(a.getId());
        dto.setNombreCompleto(a.getNombreCompleto());
        dto.setCurso(a.getCurso());
        dto.setEsSocio(a.getSocio() != null);
        return dto;
    }
}