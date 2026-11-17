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
        // 1. Permitir alumnos sin socio (socio_id NULL)
        try {
            jdbcTemplate.execute("ALTER TABLE alumno ALTER COLUMN socio_id DROP NOT NULL");
            log.info("Migracion: socio_id ahora admite NULL");
        } catch (Exception e) {
            // Si ya era nullable, la BBDD lanza excepción. Es normal.
            log.debug("socio_id ya era nullable o no requiere cambio");
        }

        // 2. Por si acaso, valor por defecto en autorizacion (no rompe si ya existe)
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE alumno ALTER COLUMN autorizacion_desplazamiento_medico SET DEFAULT FALSE");
        } catch (Exception ignored) {}
    }
}