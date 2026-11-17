package com.ampa.tesoreria.service;

import com.ampa.tesoreria.model.Ajuste;
import com.ampa.tesoreria.repository.AjusteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AjusteService {

    private final AjusteRepository ajusteRepository;

    // ============================================================
    // CONSTANTES DE CLAVES (para no equivocarnos al escribirlas)
    // ============================================================

    public static final String CUOTA_SOCIO            = "cuota.socio";
    public static final String DESCUENTO_SOCIO_GLOBAL = "descuento.socio.global";
    public static final String DATOS_AMPA_NOMBRE      = "datos.ampa.nombre";
    public static final String DATOS_AMPA_CIF         = "datos.ampa.cif";
    public static final String DATOS_AMPA_DIRECCION   = "datos.ampa.direccion";
    public static final String DATOS_AMPA_TELEFONO    = "datos.ampa.telefono";
    public static final String DATOS_AMPA_EMAIL       = "datos.ampa.email";
    public static final String DATOS_AMPA_CUENTA      = "datos.ampa.cuenta";
    public static final String IVA_ACTIVADO           = "iva.activado";
    public static final String IVA_PORCENTAJE         = "iva.porcentaje";
    public static final String SESION_TIMEOUT_MINUTOS = "sesion.timeout.minutos";

    // ============================================================
    // LECTURA TIPADA
    // ============================================================

    public String getString(String clave, String defaultValue) {
        return ajusteRepository.findByClave(clave)
                .map(Ajuste::getValor)
                .orElse(defaultValue);
    }

    public BigDecimal getBigDecimal(String clave, BigDecimal defaultValue) {
        String valor = getString(clave, null);
        if (valor == null || valor.isBlank()) return defaultValue;
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getInt(String clave, int defaultValue) {
        String valor = getString(clave, null);
        if (valor == null || valor.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String clave, boolean defaultValue) {
        String valor = getString(clave, null);
        if (valor == null || valor.isBlank()) return defaultValue;
        return Boolean.parseBoolean(valor);
    }

    // ============================================================
    // ESCRITURA
    // ============================================================

    @Transactional
    public void set(String clave, String valor, String descripcion) {
        Ajuste ajuste = ajusteRepository.findByClave(clave)
                .orElseGet(() -> Ajuste.builder()
                        .clave(clave)
                        .descripcion(descripcion)
                        .build());
        ajuste.setValor(valor);
        if (descripcion != null) {
            ajuste.setDescripcion(descripcion);
        }
        ajusteRepository.save(ajuste);
    }

    @Transactional
    public void set(String clave, String valor) {
        set(clave, valor, null);
    }

    // ============================================================
    // LISTADO COMPLETO (para la pantalla de Ajustes)
    // ============================================================

    public List<Ajuste> findAll() {
        return ajusteRepository.findAll();
    }

    public boolean exists(String clave) {
        return ajusteRepository.existsByClave(clave);
    }

    // ============================================================
    // ATAJOS ESPECÍFICOS (los que más se usan en la app)
    // ============================================================

    public BigDecimal getCuotaSocio() {
        return getBigDecimal(CUOTA_SOCIO, new BigDecimal("15.00"));
    }

    public BigDecimal getDescuentoSocioGlobal() {
        return getBigDecimal(DESCUENTO_SOCIO_GLOBAL, BigDecimal.ZERO);
    }

    public String getNombreAmpa() {
        return getString(DATOS_AMPA_NOMBRE, "AMPA");
    }

    public String getCuentaBancaria() {
        return getString(DATOS_AMPA_CUENTA, "");
    }

    public boolean isIvaActivado() {
        return getBoolean(IVA_ACTIVADO, false);
    }
}