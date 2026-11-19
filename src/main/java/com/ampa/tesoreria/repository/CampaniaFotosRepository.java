package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.CampaniaFotos;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaniaFotosRepository extends JpaRepository<CampaniaFotos, Long> {

    @EntityGraph(attributePaths = {"pedidos"})
    List<CampaniaFotos> findByCursoId(Long cursoId);

    @EntityGraph(attributePaths = {"pedidos"})
    List<CampaniaFotos> findByCursoIdAndCerradaFalse(Long cursoId);

    @EntityGraph(attributePaths = {"pedidos"})
    Optional<CampaniaFotos> findById(Long id);
}