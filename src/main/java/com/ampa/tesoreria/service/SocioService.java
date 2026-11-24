package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.SocioForm;
import com.ampa.tesoreria.model.CursoEscolar;
import com.ampa.tesoreria.model.Socio;
import com.ampa.tesoreria.model.enums.MetodoPago;
import com.ampa.tesoreria.repository.SocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocioService {

    private final SocioRepository socioRepository;
    private final CursoService cursoService;
    private final AjusteService ajusteService;

    // ------------------------------------------------------------
    // LISTADOS Y FILTROS
    // ------------------------------------------------------------

    public List<Socio> listar(Long cursoId, String q, MetodoPago metodoPago) {
        Long idCurso = (cursoId != null) ? cursoId : cursoService.getCursoActivo().getId();

        List<Socio> socios = (q != null && !q.isBlank())
                ? socioRepository.buscarPorCurso(idCurso, q)
                : socioRepository.findByCursoId(idCurso);

        if (metodoPago != null) {
            socios = socios.stream()
                    .filter(s -> s.getMetodoPago() == metodoPago)
                    .toList();
        }
        return socios;
    }

    public Socio findById(Long id) {
        return socioRepository.findByIdConAlumnos(id)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado: " + id));
    }

    public long countByCurso(Long cursoId) {
        return socioRepository.countByCursoId(cursoId);
    }

    // ------------------------------------------------------------
    // TOTALES
    // ------------------------------------------------------------

    public BigDecimal totalRecaudado(List<Socio> socios) {
        return socios.stream()
                .map(Socio::getCuotaImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPorMetodo(List<Socio> socios, MetodoPago metodo) {
        return socios.stream()
                .filter(s -> s.getMetodoPago() == metodo)
                .map(Socio::getCuotaImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long countPorMetodo(List<Socio> socios, MetodoPago metodo) {
        return socios.stream().filter(s -> s.getMetodoPago() == metodo).count();
    }

    // ------------------------------------------------------------
    // ALTA / EDICIÓN
    // ------------------------------------------------------------

    @Transactional
    public Socio crear(SocioForm form) {
        if (socioRepository.existsByDni(form.getDni())) {
            throw new IllegalArgumentException("Ya existe un socio con DNI " + form.getDni());
        }

        Socio socio = Socio.builder()
                .nombre(form.getNombre())
                .apellidos(form.getApellidos())
                .dni(form.getDni())
                .direccion(form.getDireccion())
                .telefono(form.getTelefono())
                .email(form.getEmail())
                .permisoImagen(form.isPermisoImagen())
                .numeroRecibo(form.getNumeroRecibo())
                .cuotaImporte(ajusteService.getCuotaSocio())
                .metodoPago(form.getMetodoPago())
                .fechaAlta(LocalDate.now())
                .curso(cursoService.getCursoActivo())
                .build();

        return socioRepository.save(socio);
    }

    @Transactional
    public Socio actualizar(Long id, SocioForm form) {
        Socio socio = findById(id);

        // Si cambia el DNI, comprobar que no choca con otro
        if (!socio.getDni().equals(form.getDni()) && socioRepository.existsByDni(form.getDni())) {
            throw new IllegalArgumentException("Ya existe otro socio con DNI " + form.getDni());
        }

        socio.setNombre(form.getNombre());
        socio.setApellidos(form.getApellidos());
        socio.setDni(form.getDni());
        socio.setDireccion(form.getDireccion());
        socio.setTelefono(form.getTelefono());
        socio.setEmail(form.getEmail());
        socio.setPermisoImagen(form.isPermisoImagen());
        socio.setNumeroRecibo(form.getNumeroRecibo());
        socio.setMetodoPago(form.getMetodoPago());

        return socioRepository.save(socio);
    }

    @Transactional
    public void eliminar(Long id) {
        Socio socio = findById(id);
        socioRepository.delete(socio);
    }
    public List<Socio> listarTodos() {
        return socioRepository.findByCursoId(cursoService.getCursoActivo().getId());
    }
}