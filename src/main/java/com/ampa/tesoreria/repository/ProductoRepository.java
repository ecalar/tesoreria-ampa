package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByCursoId(Long cursoId);

    List<Producto> findByCursoIdAndActivoTrue(Long cursoId);
}