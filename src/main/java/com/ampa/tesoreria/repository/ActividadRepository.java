package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Actividad;
import com.ampa.tesoreria.model.enums.TipoActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    List<Actividad> findByCursoId(Long cursoId);

    List<Actividad> findByCursoIdAndTipo(Long cursoId, TipoActividad tipo);

    List<Actividad> findByCursoIdAndNombreContainingIgnoreCase(Long cursoId, String nombre);
}