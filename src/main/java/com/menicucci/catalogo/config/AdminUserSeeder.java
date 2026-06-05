package com.menicucci.catalogo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.menicucci.catalogo.service.UsuarioService;

@Component
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUserSeeder.class);

    @Value("${app.admin.seed-password}")
    private String seedPassword;

    private final UsuarioService usuarioService;

    public AdminUserSeeder(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(String... args) throws Exception {
        String username = "admin";

        try {
            var opt = usuarioService.buscarPorUsername(username);
            if (opt.isPresent()) {
                LOG.info("Admin user '{}' already exists, skipping seed.", username);
                return;
            }

            usuarioService.criarUsuario(username, seedPassword);
            LOG.info("Admin user '{}' created by seed.", username);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message != null && message.contains("UNIQUE constraint failed: usuarios.username")) {
                LOG.info("Admin user '{}' already exists, skipping seed.", username);
                return;
            }

            LOG.error("Failed to seed admin user: {}", message);
        }
    }
}
