package com.ampa.tesoreria.repository;

import com.ampa.tesoreria.model.PagoMensual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface PagoMensualRepository extends JpaRepository<PagoMensual, Long> {

    List<PagoMensual> findByInscripcionIdOrderByMesAsc(Long inscripcionId);

    Optional<PagoMensual> findByInscripcionIdAndMes(Long inscripcionId, YearMonth mes);

    List<PagoMensual> findByPagadoFalse();
}