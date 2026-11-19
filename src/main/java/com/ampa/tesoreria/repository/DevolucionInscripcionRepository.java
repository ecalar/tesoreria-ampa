package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.DevolucionInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevolucionInscripcionRepository extends JpaRepository<DevolucionInscripcion, Long> {

    Optional<DevolucionInscripcion> findByInscripcionId(Long inscripcionId);

    List<DevolucionInscripcion> findAll();
}