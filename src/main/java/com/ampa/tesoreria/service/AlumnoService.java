package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.AlumnoForm;
import com.ampa.tesoreria.model.Alumno;
import com.ampa.tesoreria.model.ContactoEmergencia;
import com.ampa.tesoreria.model.Socio;
import com.ampa.tesoreria.repository.AlumnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;
    private final SocioService socioService;

    public Alumno findById(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado: " + id));
    }

    public List<Alumno> findBySocio(Long socioId) {
        return alumnoRepository.findBySocioId(socioId);
    }

    /** Prepara el formulario con los 4 huecos de contactos (rellenos si ya existen) */
    public AlumnoForm toForm(Alumno alumno) {
        AlumnoForm form = new AlumnoForm();
        form.setId(alumno.getId());
        form.setSocioId(alumno.getSocio().getId());
        form.setNombre(alumno.getNombre());
        form.setApellidos(alumno.getApellidos());
        form.setCurso(alumno.getCurso());
        form.setFechaNacimiento(alumno.getFechaNacimiento());
        form.setAlergias(alumno.getAlergias());
        form.setObservacionesMedicas(alumno.getObservacionesMedicas());

        // Rellenar 4 huecos
        for (int i = 0; i < 4; i++) {
            AlumnoForm.ContactoForm c = new AlumnoForm.ContactoForm();
            if (i < alumno.getContactosEmergencia().size()) {
                ContactoEmergencia ce = alumno.getContactosEmergencia().get(i);
                c.setId(ce.getId());
                c.setNombre(ce.getNombre());
                c.setTelefono(ce.getTelefono());
                c.setParentesco(ce.getParentesco());
                c.setOrden(ce.getOrden());
            } else {
                c.setOrden(i + 1);
            }
            form.getContactos().add(c);
        }
        return form;
    }

    @Transactional
    public Alumno crear(Long socioId, AlumnoForm form) {
        Socio socio = socioService.findById(socioId);

        Alumno alumno = Alumno.builder()
                .nombre(form.getNombre())
                .apellidos(form.getApellidos())
                .curso(form.getCurso())
                .fechaNacimiento(form.getFechaNacimiento())
                .alergias(form.getAlergias())
                .observacionesMedicas(form.getObservacionesMedicas())
                .socio(socio)
                .contactosEmergencia(new ArrayList<>())
                .build();

        aplicarContactos(alumno, form.getContactos());

        return alumnoRepository.save(alumno);
    }

    @Transactional
    public Alumno actualizar(Long alumnoId, AlumnoForm form) {
        Alumno alumno = findById(alumnoId);

        alumno.setNombre(form.getNombre());
        alumno.setApellidos(form.getApellidos());
        alumno.setCurso(form.getCurso());
        alumno.setFechaNacimiento(form.getFechaNacimiento());
        alumno.setAlergias(form.getAlergias());
        alumno.setObservacionesMedicas(form.getObservacionesMedicas());

        // Borrar contactos existentes y volver a crearlos
        alumno.getContactosEmergencia().clear();
        aplicarContactos(alumno, form.getContactos());

        return alumnoRepository.save(alumno);
    }

    @Transactional
    public void eliminar(Long alumnoId) {
        alumnoRepository.deleteById(alumnoId);
    }

    // ------------------------------------------------------------
    private void aplicarContactos(Alumno alumno, List<AlumnoForm.ContactoForm> contactos) {
        if (contactos == null) return;
        int orden = 1;
        for (AlumnoForm.ContactoForm c : contactos) {
            if (c.getNombre() == null || c.getNombre().isBlank()) continue;
            if (c.getTelefono() == null || c.getTelefono().isBlank()) continue;

            ContactoEmergencia ce = ContactoEmergencia.builder()
                    .nombre(c.getNombre())
                    .telefono(c.getTelefono())
                    .parentesco(c.getParentesco())
                    .orden(orden++)
                    .build();
            alumno.addContactoEmergencia(ce);
        }
    }
}