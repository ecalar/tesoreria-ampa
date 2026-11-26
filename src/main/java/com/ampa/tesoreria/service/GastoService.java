package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.GastoForm;
import com.ampa.tesoreria.model.CategoriaGasto;
import com.ampa.tesoreria.model.Gasto;
import com.ampa.tesoreria.repository.GastoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GastoService {

    private final GastoRepository gastoRepository;
    private final CategoriaGastoService categoriaService;
    private final CursoService cursoService;
    private final AjusteService ajusteService;

    @Value("${ampa.facturas.dir:data/facturas}")
    private String facturasDir;

    // ------------------------------------------------------------
    // LISTADOS
    // ------------------------------------------------------------
    public List<Gasto> listar(Long categoriaId, LocalDate desde, LocalDate hasta, String q) {
        Long cursoId = cursoService.getCursoActivo().getId();
        return gastoRepository.buscar(cursoId, categoriaId, desde, hasta,
                (q != null && q.isBlank()) ? null : q);
    }

    public Gasto findById(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gasto no encontrado: " + id));
    }

    // ------------------------------------------------------------
    // CREAR
    // ------------------------------------------------------------
    @Transactional
    public Gasto crear(GastoForm form) {
        validar(form);

        CategoriaGasto categoria = form.getCategoriaId() != null
                ? categoriaService.findById(form.getCategoriaId()) : null;

        BigDecimal importeBase = form.getImporteBase();
        BigDecimal iva = ajusteService.isIvaActivado() && form.getIvaPorcentaje() != null
                ? form.getIvaPorcentaje() : BigDecimal.ZERO;

        BigDecimal importeTotal = calcularTotal(importeBase, iva);

        Gasto gasto = Gasto.builder()
                .curso(cursoService.getCursoActivo())
                .categoria(categoria)
                .concepto(form.getConcepto())
                .importeBase(importeBase)
                .ivaPorcentaje(iva)
                .importeTotal(importeTotal)
                .fecha(form.getFecha() != null ? form.getFecha() : LocalDate.now())
                .metodoPago(form.getMetodoPago())
                .proveedor(form.getProveedor())
                .observaciones(form.getObservaciones())
                .build();

        // Guardar factura si viene
        if (form.getFactura() != null && !form.getFactura().isEmpty()) {
            guardarFactura(gasto, form.getFactura());
        }

        return gastoRepository.save(gasto);
    }

    // ------------------------------------------------------------
    // ACTUALIZAR
    // ------------------------------------------------------------
    @Transactional
    public Gasto actualizar(Long id, GastoForm form) {
        validar(form);
        Gasto gasto = findById(id);

        CategoriaGasto categoria = form.getCategoriaId() != null
                ? categoriaService.findById(form.getCategoriaId()) : null;

        BigDecimal importeBase = form.getImporteBase();
        BigDecimal iva = ajusteService.isIvaActivado() && form.getIvaPorcentaje() != null
                ? form.getIvaPorcentaje() : BigDecimal.ZERO;

        gasto.setCategoria(categoria);
        gasto.setConcepto(form.getConcepto());
        gasto.setImporteBase(importeBase);
        gasto.setIvaPorcentaje(iva);
        gasto.setImporteTotal(calcularTotal(importeBase, iva));
        gasto.setFecha(form.getFecha() != null ? form.getFecha() : gasto.getFecha());
        gasto.setMetodoPago(form.getMetodoPago());
        gasto.setProveedor(form.getProveedor());
        gasto.setObservaciones(form.getObservaciones());

        // Si viene factura nueva, reemplazar la anterior
        if (form.getFactura() != null && !form.getFactura().isEmpty()) {
            eliminarArchivo(gasto.getRutaFactura());
            guardarFactura(gasto, form.getFactura());
        }

        return gastoRepository.save(gasto);
    }

    // ------------------------------------------------------------
    // ELIMINAR
    // ------------------------------------------------------------
    @Transactional
    public void eliminar(Long id) {
        Gasto gasto = findById(id);
        eliminarArchivo(gasto.getRutaFactura());
        gastoRepository.delete(gasto);
    }

    // ------------------------------------------------------------
    // FACTURAS
    // ------------------------------------------------------------
    private void guardarFactura(Gasto gasto, MultipartFile file) {
        try {
            Path dir = Paths.get(facturasDir);
            Files.createDirectories(dir);

            String extension = "";
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "factura";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) extension = originalName.substring(dot);

            String nombreArchivo = UUID.randomUUID() + extension;
            Path destino = dir.resolve(nombreArchivo);
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            gasto.setRutaFactura(nombreArchivo);
            gasto.setNombreFacturaOriginal(originalName);
        } catch (IOException e) {
            log.error("Error guardando factura: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo guardar la factura", e);
        }
    }

    private void eliminarArchivo(String ruta) {
        if (ruta == null || ruta.isBlank()) return;
        try {
            Path path = Paths.get(facturasDir).resolve(ruta);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo de factura: {}", e.getMessage());
        }
    }

    public Path getRutaFactura(Long gastoId) {
        Gasto gasto = findById(gastoId);
        if (gasto.getRutaFactura() == null) {
            throw new IllegalArgumentException("Este gasto no tiene factura adjunta");
        }
        return Paths.get(facturasDir).resolve(gasto.getRutaFactura());
    }

    // ------------------------------------------------------------
    // CÁLCULOS
    // ------------------------------------------------------------
    private BigDecimal calcularTotal(BigDecimal base, BigDecimal ivaPct) {
        if (ivaPct == null || ivaPct.signum() == 0) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal iva = base.multiply(ivaPct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return base.add(iva).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalGastos(List<Gasto> gastos) {
        return gastos.stream().map(Gasto::getImporteTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------
    // UTILIDADES
    // ------------------------------------------------------------
    public GastoForm toForm(Gasto g) {
        GastoForm form = new GastoForm();
        form.setId(g.getId());
        form.setCategoriaId(g.getCategoria() != null ? g.getCategoria().getId() : null);
        form.setConcepto(g.getConcepto());
        form.setImporteBase(g.getImporteBase());
        form.setIvaPorcentaje(g.getIvaPorcentaje());
        form.setFecha(g.getFecha());
        form.setMetodoPago(g.getMetodoPago());
        form.setProveedor(g.getProveedor());
        form.setObservaciones(g.getObservaciones());
        form.setRutaFacturaActual(g.getRutaFactura());
        form.setNombreFacturaOriginalActual(g.getNombreFacturaOriginal());
        return form;
    }

    private void validar(GastoForm form) {
        if (form.getConcepto() == null || form.getConcepto().isBlank()) {
            throw new IllegalArgumentException("El concepto es obligatorio");
        }
        if (form.getImporteBase() == null || form.getImporteBase().signum() <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor que 0");
        }
        if (form.getMetodoPago() == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
    }
}