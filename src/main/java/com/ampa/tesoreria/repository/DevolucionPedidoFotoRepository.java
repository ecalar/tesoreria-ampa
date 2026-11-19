package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.DevolucionPedidoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DevolucionPedidoFotoRepository extends JpaRepository<DevolucionPedidoFoto, Long> {

    Optional<DevolucionPedidoFoto> findByPedidoId(Long pedidoId);
}