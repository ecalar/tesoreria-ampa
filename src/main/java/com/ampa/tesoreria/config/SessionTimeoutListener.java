package com.ampa.tesoreria.config;

import com.ampa.tesoreria.service.AjusteService;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionTimeoutListener implements HttpSessionListener {

    private final AjusteService ajusteService;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        int minutos = ajusteService.getInt(AjusteService.SESION_TIMEOUT_MINUTOS, 30);
        se.getSession().setMaxInactiveInterval(minutos * 60);
    }
}