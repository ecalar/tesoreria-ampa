package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByActividadId(Long actividadId);

    List<Inscripcion> findByAlumnoId(Long alumnoId);

    Optional<Inscripcion> findByActividadIdAndAlumnoId(Long actividadId, Long alumnoId);

    long countByActividadId(Long actividadId);

    boolean existsByActividadIdAndAlumnoId(Long actividadId, Long alumnoId);
}