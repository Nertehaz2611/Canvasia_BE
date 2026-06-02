package com.example.canvasia.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.canvasia.enums.NotificationType;
import com.example.canvasia.enums.ReferenceType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationConstraintSynchronizer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConstraintSynchronizer.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgreSql() || !notificationsTableExists()) {
            return;
        }

        dropLegacyNotificationTypeChecks();
        recreateNotificationChecks();
    }

    private boolean isPostgreSql() {
        try {
            String version = jdbcTemplate.queryForObject("select version()", String.class);
            return version != null && version.toLowerCase().contains("postgresql");
        } catch (RuntimeException ex) {
            logger.warn("Unable to detect database engine for notification check sync", ex);
            return false;
        }
    }

    private boolean notificationsTableExists() {
        Integer exists = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = current_schema()
                  and table_name = 'notifications'
                """,
                Integer.class
        );
        return exists != null && exists > 0;
    }

    private void dropLegacyNotificationTypeChecks() {
        List<String> checkNames = jdbcTemplate.queryForList(
                """
                select c.conname
                from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                join pg_namespace n on n.oid = t.relnamespace
                where c.contype = 'c'
                  and n.nspname = current_schema()
                  and t.relname = 'notifications'
                  and (
                        pg_get_constraintdef(c.oid) ilike '% type %'
                     or pg_get_constraintdef(c.oid) ilike '%(type%'
                     or pg_get_constraintdef(c.oid) ilike '% reference_type %'
                     or pg_get_constraintdef(c.oid) ilike '%(reference_type%'
                  )
                """,
                String.class
        );

        for (String checkName : checkNames) {
            jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS " + quoteIdentifier(checkName));
        }
    }

    private void recreateNotificationChecks() {
        String notificationTypes = enumLiteralList(NotificationType.values());
        String referenceTypes = enumLiteralList(ReferenceType.values());

        jdbcTemplate.execute(
                "ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (" + notificationTypes + "))"
        );
        jdbcTemplate.execute(
                "ALTER TABLE notifications ADD CONSTRAINT notifications_reference_type_check CHECK (reference_type IN (" + referenceTypes + "))"
        );
    }

    private String enumLiteralList(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(Enum::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));
    }

    private String quoteIdentifier(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }
}
