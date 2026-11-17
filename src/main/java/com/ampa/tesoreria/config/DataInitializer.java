package com.ampa.tesoreria.config;

import com.ampa.tesoreria.model.CursoEscolar;
import com.ampa.tesoreria.model.Usuario;
import com.ampa.tesoreria.model.enums.EstadoCurso;
import com.ampa.tesoreria.repository.CursoEscolarRepository;
import com.ampa.tesoreria.repository.UsuarioRepository;
import com.ampa.tesoreria.service.AjusteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CursoEscolarRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AjusteService ajusteService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        inicializarCurso();
        inicializarAjustes();
        inicializarUsuarioAdmin();
    }

    // ------------------------------------------------------------
    // 1. Curso escolar por defecto
    // ------------------------------------------------------------
    private void inicializarCurso() {
        if (cursoRepository.count() > 0) {
            log.info("Cursos ya existen, no se crea ninguno por defecto");
            return;
        }

        int anioActual = LocalDate.now().getYear();
        int anioInicio = LocalDate.now().getMonthValue() >= 9 ? anioActual : anioActual - 1;

        String nombre = anioInicio + "-" + (anioInicio + 1);

        CursoEscolar curso = CursoEscolar.builder()
                .nombre(nombre)
                .fechaInicio(LocalDate.of(anioInicio, 9, 1))
                .fechaFin(LocalDate.of(anioInicio + 1, 6, 30))
                .estado(EstadoCurso.ABIERTO)
                .build();

        cursoRepository.save(curso);
        log.info("Curso escolar inicial creado: {}", nombre);
    }

    // ------------------------------------------------------------
    // 2. Ajustes por defecto
    // ------------------------------------------------------------
    private void inicializarAjustes() {
        crearSiNoExiste(AjusteService.CUOTA_SOCIO,
                "15.00", "Cuota anual de socio en euros");

        crearSiNoExiste(AjusteService.DESCUENTO_SOCIO_GLOBAL,
                "0", "Porcentaje de descuento global para socios (0-100)");

        crearSiNoExiste(AjusteService.DATOS_AMPA_NOMBRE,
                "AMPA DEDU", "Nombre de la AMPA");

        crearSiNoExiste(AjusteService.DATOS_AMPA_CIF,
                "", "CIF de la AMPA");

        crearSiNoExiste(AjusteService.DATOS_AMPA_DIRECCION,
                "CEIP Maria Auxiliadora, Moron de la Frontera", "Direccion de la AMPA");

        crearSiNoExiste(AjusteService.DATOS_AMPA_TELEFONO,
                "", "Telefono de contacto");

        crearSiNoExiste(AjusteService.DATOS_AMPA_EMAIL,
                "ampadedu@gmail.com", "Email de contacto");

        crearSiNoExiste(AjusteService.DATOS_AMPA_CUENTA,
                "ES54 0049 1694 8023 1020 6055", "Cuenta bancaria para transferencias");

        crearSiNoExiste(AjusteService.IVA_ACTIVADO,
                "false", "Si la AMPA aplica IVA en sus actividades");

        crearSiNoExiste(AjusteService.IVA_PORCENTAJE,
                "21", "Porcentaje de IVA aplicado");

        crearSiNoExiste(AjusteService.SESION_TIMEOUT_MINUTOS,
                "30", "Minutos de inactividad antes de cerrar sesion");
    }

    private void crearSiNoExiste(String clave, String valor, String descripcion) {
        if (!ajusteService.exists(clave)) {
            ajusteService.set(clave, valor, descripcion);
            log.info("Ajuste creado: {} = {}", clave, valor);
        }
    }

    // ------------------------------------------------------------
    // 3. Usuario admin por defecto
    // ------------------------------------------------------------
    private void inicializarUsuarioAdmin() {
        if (usuarioRepository.count() > 0) {
            log.info("Usuarios ya existen, no se crea el admin por defecto");
            return;
        }

        Usuario admin = Usuario.builder()
                .username("admin")
                .password(passwordEncoder.encode("ampa2024"))
                .activo(true)
                .build();

        usuarioRepository.save(admin);
        log.warn("========================================================");
        log.warn(" Usuario admin creado con password: ampa2024");
        log.warn(" CAMBIA LA CONTRASENA EN EL PRIMER ACCESO DESDE AJUSTES");
        log.warn("========================================================");
    }
}