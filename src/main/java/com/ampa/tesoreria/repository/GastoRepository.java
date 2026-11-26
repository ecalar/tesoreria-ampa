package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Gasto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    @EntityGraph(attributePaths = {"categoria"})
    List<Gasto> findByCursoIdOrderByFechaDesc(Long cursoId);

    @EntityGraph(attributePaths = {"categoria"})
    List<Gasto> findByCursoIdAndFechaBetween(Long cursoId, LocalDate desde, LocalDate hasta);

    @EntityGraph(attributePaths = {"categoria"})
    Optional<Gasto> findById(Long id);

    @EntityGraph(attributePaths = {"categoria"})
    @Query("""
        SELECT g FROM Gasto g
        WHERE g.curso.id = :cursoId
          AND (:categoriaId IS NULL OR g.categoria.id = :categoriaId)
          AND (:desde IS NULL OR g.fecha >= :desde)
          AND (:hasta IS NULL OR g.fecha <= :hasta)
          AND (:q IS NULL OR LOWER(g.concepto) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(COALESCE(g.proveedor, '')) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY g.fecha DESC
        """)
    List<Gasto> buscar(@Param("cursoId") Long cursoId,
                       @Param("categoriaId") Long categoriaId,
                       @Param("desde") LocalDate desde,
                       @Param("hasta") LocalDate hasta,
                       @Param("q") String q);
}