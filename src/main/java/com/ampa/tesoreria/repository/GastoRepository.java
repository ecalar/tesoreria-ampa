package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findByCursoIdOrderByFechaDesc(Long cursoId);

    List<Gasto> findByCursoIdAndFechaBetween(Long cursoId, LocalDate desde, LocalDate hasta);

    List<Gasto> findByCategoriaId(Long categoriaId);
}