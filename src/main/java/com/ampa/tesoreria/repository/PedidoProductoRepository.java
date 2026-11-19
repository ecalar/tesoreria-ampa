package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.PedidoProducto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoProductoRepository extends JpaRepository<PedidoProducto, Long> {

    @EntityGraph(attributePaths = {"producto", "alumno", "alumno.socio"})
    List<PedidoProducto> findByProductoId(Long productoId);

    @EntityGraph(attributePaths = {"producto", "alumno", "alumno.socio"})
    List<PedidoProducto> findByAlumnoId(Long alumnoId);

    long countByProductoId(Long productoId);
}