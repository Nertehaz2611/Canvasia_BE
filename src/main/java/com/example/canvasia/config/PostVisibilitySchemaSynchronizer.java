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

import com.example.canvasia.enums.PostVisibility;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostVisibilitySchemaSynchronizer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PostVisibilitySchemaSynchronizer.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgreSql() || !postsTableExists()) {
            return;
        }

        ensureVisibilityColumn();
        ensureVisibilityConstraint();
        ensurePostAllowedViewersTable();
    }

    private boolean isPostgreSql() {
        try {
            String version = jdbcTemplate.queryForObject("select version()", String.class);
            return version != null && version.toLowerCase().contains("postgresql");
        } catch (RuntimeException ex) {
            logger.warn("Unable to detect database engine for post visibility sync", ex);
            return false;
        }
    }

    private boolean postsTableExists() {
        Integer exists = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = current_schema()
                  and table_name = 'posts'
                """,
                Integer.class
        );
        return exists != null && exists > 0;
    }

    private void ensureVisibilityColumn() {
        jdbcTemplate.execute("ALTER TABLE posts ADD COLUMN IF NOT EXISTS visibility varchar(20)");
        jdbcTemplate.execute("UPDATE posts SET visibility = 'PUBLIC' WHERE visibility IS NULL");
        jdbcTemplate.execute("ALTER TABLE posts ALTER COLUMN visibility SET DEFAULT 'PUBLIC'");
        jdbcTemplate.execute("ALTER TABLE posts ALTER COLUMN visibility SET NOT NULL");
    }

    private void ensureVisibilityConstraint() {
        List<String> checkNames = jdbcTemplate.queryForList(
                """
                select c.conname
                from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                join pg_namespace n on n.oid = t.relnamespace
                where c.contype = 'c'
                  and n.nspname = current_schema()
                  and t.relname = 'posts'
                  and (
                        pg_get_constraintdef(c.oid) ilike '% visibility %'
                     or pg_get_constraintdef(c.oid) ilike '%(visibility%'
                  )
                """,
                String.class
        );

        for (String checkName : checkNames) {
            jdbcTemplate.execute("ALTER TABLE posts DROP CONSTRAINT IF EXISTS " + quoteIdentifier(checkName));
        }

        String visibilityValues = enumLiteralList(PostVisibility.values());
        jdbcTemplate.execute(
                "ALTER TABLE posts ADD CONSTRAINT posts_visibility_check CHECK (visibility IN (" + visibilityValues + "))"
        );
    }

    private void ensurePostAllowedViewersTable() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS post_allowed_viewers (
                    id uuid PRIMARY KEY,
                    post_id uuid NOT NULL,
                    user_id uuid NOT NULL,
                    created_at timestamp,
                    updated_at timestamp,
                    CONSTRAINT uk_post_allowed_viewers_post_user UNIQUE (post_id, user_id),
                    CONSTRAINT fk_post_allowed_viewers_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                    CONSTRAINT fk_post_allowed_viewers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """
        );

        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_post_allowed_viewers_post ON post_allowed_viewers(post_id)"
        );
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_post_allowed_viewers_user ON post_allowed_viewers(user_id)"
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
