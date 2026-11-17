package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Alumno;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {

    @EntityGraph(attributePaths = {"contactosEmergencia", "socio"})
    List<Alumno> findBySocioId(Long socioId);

    @EntityGraph(attributePaths = {"contactosEmergencia"})
    List<Alumno> findBySocioIsNull();

    List<Alumno> findByCurso(String curso);

    @EntityGraph(attributePaths = {"contactosEmergencia", "socio"})
    @Query("SELECT a FROM Alumno a WHERE a.id = :id")
    Optional<Alumno> findByIdConDetalles(@Param("id") Long id);

    @Query("""
        SELECT a FROM Alumno a
        WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(a.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
        """)
    List<Alumno> buscar(@Param("q") String q);

    @Query("""
        SELECT a FROM Alumno a
        WHERE a.socio.curso.id = :cursoId
          AND (LOWER(a.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.curso) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    List<Alumno> buscarPorCurso(@Param("cursoId") Long cursoId, @Param("q") String q);
}