package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.PedidoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoFotoRepository extends JpaRepository<PedidoFoto, Long> {

    List<PedidoFoto> findByCampaniaId(Long campaniaId);

    List<PedidoFoto> findByAlumnoId(Long alumnoId);

    Optional<PedidoFoto> findByCampaniaIdAndAlumnoId(Long campaniaId, Long alumnoId);

    long countByCampaniaId(Long campaniaId);
}