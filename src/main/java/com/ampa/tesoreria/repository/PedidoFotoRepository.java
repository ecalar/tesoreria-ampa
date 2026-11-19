package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.PedidoFoto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoFotoRepository extends JpaRepository<PedidoFoto, Long> {

    @EntityGraph(attributePaths = {"alumno", "alumno.socio", "grupos", "hermanos"})
    List<PedidoFoto> findByCampaniaId(Long campaniaId);

    @EntityGraph(attributePaths = {"alumno", "campania"})
    List<PedidoFoto> findByAlumnoId(Long alumnoId);

    @EntityGraph(attributePaths = {"alumno", "alumno.socio", "grupos", "hermanos"})
    Optional<PedidoFoto> findByCampaniaIdAndAlumnoId(Long campaniaId, Long alumnoId);

    @EntityGraph(attributePaths = {"alumno", "campania", "grupos", "hermanos"})
    Optional<PedidoFoto> findById(Long id);

    long countByCampaniaId(Long campaniaId);
}