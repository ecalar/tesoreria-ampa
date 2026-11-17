package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Actividad;
import com.ampa.tesoreria.model.enums.TipoActividad;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    @EntityGraph(attributePaths = {"inscripciones"})
    List<Actividad> findByCursoId(Long cursoId);

    @EntityGraph(attributePaths = {"inscripciones"})
    List<Actividad> findByCursoIdAndTipo(Long cursoId, TipoActividad tipo);

    @EntityGraph(attributePaths = {"inscripciones"})
    List<Actividad> findByCursoIdAndNombreContainingIgnoreCase(Long cursoId, String nombre);

    @EntityGraph(attributePaths = {"inscripciones"})
    Optional<Actividad> findById(Long id);
}