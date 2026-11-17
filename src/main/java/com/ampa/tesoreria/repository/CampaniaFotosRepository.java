package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.CampaniaFotos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaniaFotosRepository extends JpaRepository<CampaniaFotos, Long> {

    List<CampaniaFotos> findByCursoId(Long cursoId);

    List<CampaniaFotos> findByCursoIdAndCerradaFalse(Long cursoId);
}