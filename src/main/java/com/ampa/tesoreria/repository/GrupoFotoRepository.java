package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.GrupoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrupoFotoRepository extends JpaRepository<GrupoFoto, Long> {

    List<GrupoFoto> findByPedidoId(Long pedidoId);
}