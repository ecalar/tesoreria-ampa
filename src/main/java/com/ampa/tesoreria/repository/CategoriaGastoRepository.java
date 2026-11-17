package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.CategoriaGasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaGastoRepository extends JpaRepository<CategoriaGasto, Long> {

    List<CategoriaGasto> findByActivaTrue();

    Optional<CategoriaGasto> findByNombre(String nombre);
}