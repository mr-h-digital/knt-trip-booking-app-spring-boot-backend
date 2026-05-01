package com.kntransport.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Applies one-off DDL fixes that ddl-auto=update cannot handle
 * (e.g. dropping NOT NULL constraints on existing columns).
 * Each statement is idempotent and safe to run on every startup.
 */
@Configuration
public class SchemaMigrationConfig implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public SchemaMigrationConfig(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Allow quotes.accepted to be NULL (pending commuter response).
        // ddl-auto=update never drops constraints, so we patch it manually.
        jdbc.execute("ALTER TABLE quotes ALTER COLUMN accepted DROP NOT NULL");

        // Clear any avatar_url values that still point to the old local /uploads/ path.
        // These were saved before Cloudflare R2 was configured; Railway's ephemeral
        // filesystem no longer serves them, causing 500s on every profile load.
        // Clearing them makes the app fall back to the initials avatar gracefully.
        jdbc.execute(
            "UPDATE users SET avatar_url = NULL WHERE avatar_url LIKE '/uploads/%'"
        );
    }
}
