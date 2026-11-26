package com.ampa.tesoreria.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${ampa.facturas.dir:data/facturas}")
    private String facturasDir;

    @Value("${ampa.backups.dir:data/backups}")
    private String backupsDir;

    /**
     * Genera un ZIP con la base de datos y las facturas.
     * Devuelve la ruta del ZIP creado.
     */
    public Path generarBackup(String motivo) {
        try {
            Path dirBackups = Paths.get(backupsDir);
            Files.createDirectories(dirBackups);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String nombreZip = "backup_" + timestamp + "_" + motivo + ".zip";
            Path zipPath = dirBackups.resolve(nombreZip);

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {

                // 1. Base de datos (archivo H2)
                Path dbFile = extraerRutaDb();
                if (dbFile != null && Files.exists(dbFile)) {
                    addFileToZip(zos, dbFile, dbFile.getFileName().toString());
                    log.info("Backup: BD incluida ({})", dbFile.getFileName());
                }

                // 2. Facturas (si existen)
                Path dirFacturas = Paths.get(facturasDir);
                if (Files.exists(dirFacturas)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirFacturas)) {
                        for (Path f : stream) {
                            if (Files.isRegularFile(f)) {
                                addFileToZip(zos, f, "facturas/" + f.getFileName());
                            }
                        }
                    }
                }
            }

            log.info("Backup generado: {}", zipPath.toAbsolutePath());
            return zipPath;

        } catch (IOException e) {
            log.error("Error generando backup: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo generar el backup", e);
        }
    }

    /** Extrae la ruta física del archivo H2 desde la URL de conexión */
    private Path extraerRutaDb() {
        try {
            // jdbc:h2:file:./data/ampa_db;AUTO_SERVER=TRUE
            String url = dbUrl;
            int idx = url.indexOf("file:");
            if (idx < 0) return null;
            String ruta = url.substring(idx + 5);
            int semi = ruta.indexOf(';');
            if (semi >= 0) ruta = ruta.substring(0, semi);
            Path dbFile = Paths.get(ruta + ".mv.db");
            return dbFile.toAbsolutePath();
        } catch (Exception e) {
            log.warn("No se pudo extraer la ruta de la BD: {}", e.getMessage());
            return null;
        }
    }

    private void addFileToZip(ZipOutputStream zos, Path file, String entryName) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zos);
        zos.closeEntry();
    }

    /** Lista los backups existentes (nombre + tamaño + fecha) */
    public java.util.List<Path> listarBackups() {
        try {
            Path dir = Paths.get(backupsDir);
            if (!Files.exists(dir)) return java.util.List.of();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.zip")) {
                java.util.List<Path> result = new java.util.ArrayList<>();
                for (Path p : stream) result.add(p);
                result.sort((a, b) -> b.getFileName().toString()
                        .compareTo(a.getFileName().toString())); // más reciente primero
                return result;
            }
        } catch (IOException e) {
            return java.util.List.of();
        }
    }
}