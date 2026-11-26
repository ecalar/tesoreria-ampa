package com.ampa.tesoreria.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DbMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 1. Permitir alumnos sin socio
        try {
            jdbcTemplate.execute("ALTER TABLE alumno ALTER COLUMN socio_id DROP NOT NULL");
            log.info("Migracion: socio_id ahora admite NULL");
        } catch (Exception e) {
            log.debug("socio_id ya era nullable o no requiere cambio");
        }

        // 2. Valor por defecto en autorizacion
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE alumno ALTER COLUMN autorizacion_desplazamiento_medico SET DEFAULT FALSE");
        } catch (Exception ignored) {}

        // 3. La columna 'importe' de gasto (legacy) ahora admite NULL
        //    porque hemos pasado a importe_base / importe_total
        try {
            jdbcTemplate.execute("ALTER TABLE gasto ALTER COLUMN importe DROP NOT NULL");
            log.info("Migracion: gasto.importe ahora admite NULL");
        } catch (Exception e) {
            log.debug("gasto.importe ya era nullable o no existe");
        }
    }
}