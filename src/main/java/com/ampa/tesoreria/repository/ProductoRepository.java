package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = {"pedidos", "pedidos.alumno"})
    List<Producto> findByCursoId(Long cursoId);

    @EntityGraph(attributePaths = {"pedidos", "pedidos.alumno"})
    List<Producto> findByCursoIdAndActivoTrue(Long cursoId);

    @EntityGraph(attributePaths = {"pedidos", "pedidos.alumno"})
    List<Producto> findByCursoIdAndNombreContainingIgnoreCase(Long cursoId, String nombre);

    @EntityGraph(attributePaths = {"pedidos", "pedidos.alumno"})
    Optional<Producto> findById(Long id);
}