package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.HermanoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HermanoFotoRepository extends JpaRepository<HermanoFoto, Long> {

    List<HermanoFoto> findByPedidoId(Long pedidoId);
}