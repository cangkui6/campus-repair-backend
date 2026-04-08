package com.graduation.repair.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaPatchConfig {

    @Bean
    public ApplicationRunner schemaPatchRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            ensureColumn(jdbcTemplate, "llm_parse_audit_log", "provider_name",
                    "ALTER TABLE llm_parse_audit_log ADD COLUMN provider_name VARCHAR(50) NOT NULL DEFAULT 'ZHIPU_GLM'");
            ensureColumn(jdbcTemplate, "llm_parse_review_queue", "reason_code",
                    "ALTER TABLE llm_parse_review_queue ADD COLUMN reason_code VARCHAR(50) NOT NULL DEFAULT 'EMPTY_FAULT'");
            ensureColumn(jdbcTemplate, "dispatch_record", "score_version",
                    "ALTER TABLE dispatch_record ADD COLUMN score_version INT");
            ensureColumn(jdbcTemplate, "maintenance_worker", "avg_complete_hours",
                    "ALTER TABLE maintenance_worker ADD COLUMN avg_complete_hours DECIMAL(8,2) NOT NULL DEFAULT 24.00");
            ensureColumn(jdbcTemplate, "maintenance_worker", "accept_rate",
                    "ALTER TABLE maintenance_worker ADD COLUMN accept_rate DECIMAL(6,4) NOT NULL DEFAULT 0.8500");
            ensureColumn(jdbcTemplate, "maintenance_worker", "completed_ticket_count",
                    "ALTER TABLE maintenance_worker ADD COLUMN completed_ticket_count INT NOT NULL DEFAULT 0");
            ensureColumn(jdbcTemplate, "maintenance_worker", "reassign_count",
                    "ALTER TABLE maintenance_worker ADD COLUMN reassign_count INT NOT NULL DEFAULT 0");
            ensureColumn(jdbcTemplate, "maintenance_worker", "last_active_at",
                    "ALTER TABLE maintenance_worker ADD COLUMN last_active_at DATETIME NULL");
        };
    }

    private void ensureColumn(JdbcTemplate jdbcTemplate, String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
