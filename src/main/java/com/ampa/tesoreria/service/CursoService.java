package com.ampa.tesoreria.service;

import com.ampa.tesoreria.model.CursoEscolar;
import com.ampa.tesoreria.model.enums.EstadoCurso;
import com.ampa.tesoreria.repository.CursoEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CursoService {

    private final CursoEscolarRepository cursoRepository;

    public List<CursoEscolar> findAll() {
        return cursoRepository.findAllByOrderByFechaInicioDesc();
    }

    public CursoEscolar getCursoActivo() {
        return cursoRepository.findFirstByEstadoOrderByFechaInicioDesc(EstadoCurso.ABIERTO)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay ningun curso escolar abierto. Crea uno en Ajustes > Cursos."));
    }

    public CursoEscolar findById(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado: " + id));
    }

    @Transactional
    public CursoEscolar crearCurso(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
        if (cursoRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe un curso con el nombre: " + nombre);
        }
        CursoEscolar curso = CursoEscolar.builder()
                .nombre(nombre)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .estado(EstadoCurso.ABIERTO)
                .build();
        return cursoRepository.save(curso);
    }

    @Transactional
    public void cerrarCurso(Long id) {
        CursoEscolar curso = findById(id);
        if (curso.getEstado() == EstadoCurso.CERRADO) {
            return;
        }
        curso.setEstado(EstadoCurso.CERRADO);
        cursoRepository.save(curso);
    }

    public boolean existeAlgunCursoAbierto() {
        return !cursoRepository.findByEstado(EstadoCurso.ABIERTO).isEmpty();
    }
}