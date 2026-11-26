package com.ampa.tesoreria.service;

import com.ampa.tesoreria.dto.CategoriaGastoForm;
import com.ampa.tesoreria.model.CategoriaGasto;
import com.ampa.tesoreria.repository.CategoriaGastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaGastoService {

    private final CategoriaGastoRepository categoriaRepository;

    public List<CategoriaGasto> listarTodas() {
        return categoriaRepository.findAll();
    }

    public List<CategoriaGasto> listarActivas() {
        return categoriaRepository.findByActivaTrue();
    }

    public CategoriaGasto findById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));
    }

    @Transactional
    public CategoriaGasto crear(CategoriaGastoForm form) {
        if (categoriaRepository.existsByNombre(form.getNombre())) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
        CategoriaGasto c = CategoriaGasto.builder()
                .nombre(form.getNombre())
                .descripcion(form.getDescripcion())
                .activa(form.isActiva())
                .build();
        return categoriaRepository.save(c);
    }

    @Transactional
    public CategoriaGasto actualizar(Long id, CategoriaGastoForm form) {
        CategoriaGasto c = findById(id);
        if (!c.getNombre().equals(form.getNombre())
                && categoriaRepository.existsByNombre(form.getNombre())) {
            throw new IllegalArgumentException("Ya existe otra categoría con ese nombre");
        }
        c.setNombre(form.getNombre());
        c.setDescripcion(form.getDescripcion());
        c.setActiva(form.isActiva());
        return categoriaRepository.save(c);
    }

    @Transactional
    public void eliminar(Long id) {
        CategoriaGasto c = findById(id);
        if (c.getGastos() != null && !c.getGastos().isEmpty()) {
            throw new IllegalStateException(
                    "No se puede borrar una categoría con gastos asociados. " +
                            "Desactívala en lugar de borrarla.");
        }
        categoriaRepository.delete(c);
    }

    public CategoriaGastoForm toForm(CategoriaGasto c) {
        CategoriaGastoForm form = new CategoriaGastoForm();
        form.setId(c.getId());
        form.setNombre(c.getNombre());
        form.setDescripcion(c.getDescripcion());
        form.setActiva(c.isActiva());
        return form;
    }

    /** Crea las categorías por defecto si la tabla está vacía */
    @Transactional
    public void inicializarPorDefecto() {
        if (categoriaRepository.count() > 0) return;
        String[][] defaults = {
                {"Material", "Material fungible y de oficina"},
                {"Fotógrafo", "Servicios de fotografía"},
                {"Regalos", "Regalos y detalles"},
                {"Actividades", "Gastos de actividades extraescolares"},
                {"Mantenimiento", "Reparaciones y mantenimiento"},
                {"Otros", "Otros gastos"}
        };
        for (String[] d : defaults) {
            CategoriaGasto c = CategoriaGasto.builder()
                    .nombre(d[0])
                    .descripcion(d[1])
                    .activa(true)
                    .build();
            categoriaRepository.save(c);
        }
    }
}