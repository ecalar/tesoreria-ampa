package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.DevolucionPedidoProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DevolucionPedidoProductoRepository
        extends JpaRepository<DevolucionPedidoProducto, Long> {

    Optional<DevolucionPedidoProducto> findByPedidoId(Long pedidoId);
}