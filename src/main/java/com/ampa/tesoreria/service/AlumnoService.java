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
        return alumnoRepository.findByIdConDetalles(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado: " + id));
    }

    public List<Alumno> findBySocio(Long socioId) {
        return alumnoRepository.findBySocioId(socioId);
    }

    public List<Alumno> listarTodos() {
        return alumnoRepository.findAll();
    }

    /** Prepara el formulario de edicion a partir de un alumno existente */
    public AlumnoForm toForm(Alumno alumno) {
        AlumnoForm form = new AlumnoForm();
        form.setId(alumno.getId());
        form.setSocioId(alumno.getSocio() != null ? alumno.getSocio().getId() : null);
        form.setNombre(alumno.getNombre());
        form.setApellidos(alumno.getApellidos());
        form.setCurso(alumno.getCurso());
        form.setFechaNacimiento(alumno.getFechaNacimiento());
        form.setDireccion(alumno.getDireccion());
        form.setCentroEducativo(alumno.getCentroEducativo());
        form.setAlergias(alumno.getAlergias());
        form.setObservacionesMedicas(alumno.getObservacionesMedicas());
        form.setAutorizacionDesplazamientoMedico(alumno.isAutorizacionDesplazamientoMedico());
        form.setNombreResponsable(alumno.getNombreResponsable());
        form.setDniResponsable(alumno.getDniResponsable());
        form.setTelefonoResponsable(alumno.getTelefonoResponsable());
        form.setEmailResponsable(alumno.getEmailResponsable());
        form.setParentescoResponsable(alumno.getParentescoResponsable());

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

    /** Crea un alumno asociado a un socio (flujo normal) */
    @Transactional
    public Alumno crear(Long socioId, AlumnoForm form) {
        Socio socio = socioService.findById(socioId);
        Alumno alumno = Alumno.builder()
                .nombre(form.getNombre())
                .apellidos(form.getApellidos())
                .curso(form.getCurso())
                .fechaNacimiento(form.getFechaNacimiento())
                .direccion(form.getDireccion())
                .centroEducativo(form.getCentroEducativo())
                .alergias(form.getAlergias())
                .observacionesMedicas(form.getObservacionesMedicas())
                .autorizacionDesplazamientoMedico(form.isAutorizacionDesplazamientoMedico())
                .socio(socio)
                .contactosEmergencia(new ArrayList<>())
                .build();
        aplicarContactos(alumno, form.getContactos());
        return alumnoRepository.save(alumno);
    }

    /** Crea un alumno SIN socio (no socio del AMPA) con sus datos de responsable legal */
    @Transactional
    public Alumno crearNoSocio(AlumnoForm form) {
        if (form.getNombreResponsable() == null || form.getNombreResponsable().isBlank()) {
            throw new IllegalArgumentException("El nombre del responsable legal es obligatorio");
        }
        if (form.getEmailResponsable() == null || form.getEmailResponsable().isBlank()) {
            throw new IllegalArgumentException("El email del responsable legal es obligatorio");
        }

        Alumno alumno = Alumno.builder()
                .nombre(form.getNombre())
                .apellidos(form.getApellidos())
                .curso(form.getCurso())
                .fechaNacimiento(form.getFechaNacimiento())
                .direccion(form.getDireccion())
                .centroEducativo(form.getCentroEducativo())
                .alergias(form.getAlergias())
                .observacionesMedicas(form.getObservacionesMedicas())
                .autorizacionDesplazamientoMedico(form.isAutorizacionDesplazamientoMedico())
                .nombreResponsable(form.getNombreResponsable())
                .dniResponsable(form.getDniResponsable())
                .telefonoResponsable(form.getTelefonoResponsable())
                .emailResponsable(form.getEmailResponsable())
                .parentescoResponsable(form.getParentescoResponsable())
                .socio(null)
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
        alumno.setDireccion(form.getDireccion());
        alumno.setCentroEducativo(form.getCentroEducativo());
        alumno.setAlergias(form.getAlergias());
        alumno.setObservacionesMedicas(form.getObservacionesMedicas());
        alumno.setAutorizacionDesplazamientoMedico(form.isAutorizacionDesplazamientoMedico());

        // Si es no socio, actualizamos tambien los datos del responsable
        if (alumno.getSocio() == null) {
            alumno.setNombreResponsable(form.getNombreResponsable());
            alumno.setDniResponsable(form.getDniResponsable());
            alumno.setTelefonoResponsable(form.getTelefonoResponsable());
            alumno.setEmailResponsable(form.getEmailResponsable());
            alumno.setParentescoResponsable(form.getParentescoResponsable());
        }

        alumno.getContactosEmergencia().clear();
        aplicarContactos(alumno, form.getContactos());
        return alumnoRepository.save(alumno);
    }

    @Transactional
    public void eliminar(Long alumnoId) {
        alumnoRepository.deleteById(alumnoId);
    }

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