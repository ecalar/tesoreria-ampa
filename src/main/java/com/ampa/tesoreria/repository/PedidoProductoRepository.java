package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.PedidoProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoProductoRepository extends JpaRepository<PedidoProducto, Long> {

    List<PedidoProducto> findByProductoId(Long productoId);

    List<PedidoProducto> findByAlumnoId(Long alumnoId);
}