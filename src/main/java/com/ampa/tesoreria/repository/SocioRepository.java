package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Socio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocioRepository extends JpaRepository<Socio, Long> {

    List<Socio> findByCursoId(Long cursoId);

    Optional<Socio> findByDni(String dni);

    boolean existsByDni(String dni);

    long countByCursoId(Long cursoId);

    @Query("""
        SELECT s FROM Socio s
        WHERE s.curso.id = :cursoId
          AND (LOWER(s.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(s.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(s.dni) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    List<Socio> buscarPorCurso(@Param("cursoId") Long cursoId, @Param("q") String q);
}