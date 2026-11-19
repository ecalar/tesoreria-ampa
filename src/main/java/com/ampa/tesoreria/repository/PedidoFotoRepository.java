package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.PedidoFoto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoFotoRepository extends JpaRepository<PedidoFoto, Long> {

    @EntityGraph(attributePaths = {"alumno", "alumno.socio", "grupos"})
    List<PedidoFoto> findByCampaniaId(Long campaniaId);

    @EntityGraph(attributePaths = {"alumno", "campania"})
    List<PedidoFoto> findByAlumnoId(Long alumnoId);

    @EntityGraph(attributePaths = {"alumno", "alumno.socio", "grupos"})
    Optional<PedidoFoto> findByCampaniaIdAndAlumnoId(Long campaniaId, Long alumnoId);

    @EntityGraph(attributePaths = {"alumno", "campania", "grupos"})
    Optional<PedidoFoto> findById(Long id);

    long countByCampaniaId(Long campaniaId);

    /** Carga los hermanos de un pedido (segunda consulta, evita el MultipleBagFetch) */
    @Query("SELECT h FROM HermanoFoto h WHERE h.pedido.id = :pedidoId")
    List<com.ampa.tesoreria.model.HermanoFoto> findHermanosByPedidoId(@Param("pedidoId") Long pedidoId);
}