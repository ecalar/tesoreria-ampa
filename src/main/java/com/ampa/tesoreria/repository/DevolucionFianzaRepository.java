package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.DevolucionFianza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DevolucionFianzaRepository extends JpaRepository<DevolucionFianza, Long> {

    Optional<DevolucionFianza> findByInscripcionId(Long inscripcionId);
}