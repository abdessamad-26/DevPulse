package com.devpulse.configuration;

import com.devpulse.entity.Role;
import com.devpulse.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures the three RBAC roles (ADMIN, DEVELOPER, VIEWER) exist at startup.
 *
 * This runs regardless of whether Flyway is enabled: in local/dev profiles
 * (Flyway disabled, Hibernate ddl-auto=update) there is no migration to seed
 * the roles table, so registration would otherwise fail with "role not found".
 * In production (Flyway enabled), V2__seed_roles.sql already inserts them,
 * and this seeder becomes a harmless no-op thanks to the existence check.
 *
 * @Order(1): must run before DemoDataSeeder, which depends on these roles existing.
 */
@Component
@Order(1)
public class RoleSeeder implements CommandLineRunner {

    private static final List<String> DEFAULT_ROLES = List.of("ADMIN", "DEVELOPER", "VIEWER");

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        DEFAULT_ROLES.forEach(name -> {
            if (roleRepository.findByName(name).isEmpty()) {
                Role role = new Role();
                role.setName(name);
                roleRepository.save(role);
            }
        });
    }
}
