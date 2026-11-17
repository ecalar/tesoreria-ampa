package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Inscripcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    @EntityGraph(attributePaths = {"alumno", "alumno.socio", "pagosMensuales"})
    List<Inscripcion> findByActividadId(Long actividadId);

    @EntityGraph(attributePaths = {"alumno", "actividad", "pagosMensuales"})
    List<Inscripcion> findByAlumnoId(Long alumnoId);

    @EntityGraph(attributePaths = {"alumno", "actividad", "pagosMensuales"})
    Optional<Inscripcion> findByActividadIdAndAlumnoId(Long actividadId, Long alumnoId);

    long countByActividadId(Long actividadId);

    boolean existsByActividadIdAndAlumnoId(Long actividadId, Long alumnoId);

    @EntityGraph(attributePaths = {"alumno", "actividad", "pagosMensuales", "devolucionFianza"})
    Optional<Inscripcion> findById(Long id);
}