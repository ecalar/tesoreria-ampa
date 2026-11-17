package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.CursoEscolar;
import com.ampa.tesoreria.model.enums.EstadoCurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursoEscolarRepository extends JpaRepository<CursoEscolar, Long> {

    Optional<CursoEscolar> findByNombre(String nombre);

    List<CursoEscolar> findByEstado(EstadoCurso estado);

    Optional<CursoEscolar> findFirstByEstadoOrderByFechaInicioDesc(EstadoCurso estado);

    List<CursoEscolar> findAllByOrderByFechaInicioDesc();

    boolean existsByNombre(String nombre);
}