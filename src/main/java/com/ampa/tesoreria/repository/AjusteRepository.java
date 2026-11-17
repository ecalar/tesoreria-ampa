package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Ajuste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AjusteRepository extends JpaRepository<Ajuste, Long> {

    Optional<Ajuste> findByClave(String clave);

    boolean existsByClave(String clave);
}