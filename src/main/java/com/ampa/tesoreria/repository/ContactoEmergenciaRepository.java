package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.ContactoEmergencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactoEmergenciaRepository extends JpaRepository<ContactoEmergencia, Long> {

    List<ContactoEmergencia> findByAlumnoIdOrderByOrdenAsc(Long alumnoId);
}