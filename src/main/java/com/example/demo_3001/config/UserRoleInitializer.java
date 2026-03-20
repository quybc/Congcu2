package com.example.demo_3001.config;

import com.example.demo_3001.model.RoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
@Component
@RequiredArgsConstructor
public class UserRoleInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        ensureUserRolesTable();
        migrateFromAppUsersRoleId();
        migrateFromLegacyRoleName();
        assignRoleForMissingUsers();
        normalizeInvalidRoleId();
        ensureAtLeastOneAdmin();
        ensureForeignKeyUserRolesUserId();
        dropLegacyColumnsInAppUsers();
    }

    private void ensureUserRolesTable() {
        if (columnExists("user_roles", "id")) {
            dropOldAppUsersRoleForeignKey();
            jdbcTemplate.execute("DROP TABLE user_roles");
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_roles (
                    user_id BIGINT NOT NULL PRIMARY KEY,
                    role_id BIGINT NOT NULL
                )
                """);
    }

    private void migrateFromAppUsersRoleId() {
        if (columnExists("app_users", "role_id")) {
            jdbcTemplate.update("""
                    INSERT INTO user_roles(user_id, role_id)
                    SELECT a.id, CASE
                        WHEN a.role_id IN (1,2,3) THEN a.role_id
                        ELSE 3
                    END
                    FROM app_users a
                    LEFT JOIN user_roles ur ON ur.user_id = a.id
                    WHERE ur.user_id IS NULL
                    """);
        }
    }

    private void migrateFromLegacyRoleName() {
        if (columnExists("app_users", "role")) {
            jdbcTemplate.update("""
                    INSERT INTO user_roles(user_id, role_id)
                    SELECT a.id,
                           CASE UPPER(a.role)
                               WHEN 'ADMIN' THEN 1
                               WHEN 'MANAGER' THEN 2
                               ELSE 3
                           END
                    FROM app_users a
                    LEFT JOIN user_roles ur ON ur.user_id = a.id
                    WHERE ur.user_id IS NULL
                    """);
        }
    }

    private void assignRoleForMissingUsers() {
        Long missingCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM app_users a
                LEFT JOIN user_roles ur ON ur.user_id = a.id
                WHERE ur.user_id IS NULL
                """, Long.class);
        if (missingCount == null || missingCount == 0L) {
            return;
        }

        Long adminCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_roles WHERE role_id = 1", Long.class);
        if (adminCount != null && adminCount == 0L) {
            jdbcTemplate.update("""
                    INSERT INTO user_roles(user_id, role_id)
                    SELECT a.id,
                           CASE WHEN a.id = (SELECT MIN(id) FROM app_users) THEN 1 ELSE 3 END
                    FROM app_users a
                    LEFT JOIN user_roles ur ON ur.user_id = a.id
                    WHERE ur.user_id IS NULL
                    """);
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO user_roles(user_id, role_id)
                SELECT a.id, 3
                FROM app_users a
                LEFT JOIN user_roles ur ON ur.user_id = a.id
                WHERE ur.user_id IS NULL
                """);
    }

    private void normalizeInvalidRoleId() {
        jdbcTemplate.update("UPDATE user_roles SET role_id = 3 WHERE role_id NOT IN (1,2,3)");
    }

    private void ensureAtLeastOneAdmin() {
        Long adminCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_roles WHERE role_id = 1", Long.class);
        if (adminCount != null && adminCount == 0L) {
            jdbcTemplate.update("""
                    UPDATE user_roles
                    SET role_id = 1
                    WHERE user_id = (SELECT user_id FROM (SELECT user_id FROM user_roles ORDER BY user_id ASC LIMIT 1) t)
                    """);
        }
    }

    private void ensureForeignKeyUserRolesUserId() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'user_roles'
                  AND COLUMN_NAME = 'user_id'
                  AND REFERENCED_TABLE_NAME = 'app_users'
                """, Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES app_users(id)");
        }
    }

    private void dropLegacyColumnsInAppUsers() {
        dropOldAppUsersRoleForeignKey();
        if (columnExists("app_users", "role_id")) {
            jdbcTemplate.execute("ALTER TABLE app_users DROP COLUMN role_id");
        }
        if (columnExists("app_users", "role")) {
            jdbcTemplate.execute("ALTER TABLE app_users DROP COLUMN role");
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private void dropOldAppUsersRoleForeignKey() {
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList("""
                SELECT tc.CONSTRAINT_NAME
                FROM information_schema.TABLE_CONSTRAINTS tc
                JOIN information_schema.KEY_COLUMN_USAGE kcu
                  ON tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                 AND tc.TABLE_NAME = kcu.TABLE_NAME
                 AND tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                WHERE tc.TABLE_SCHEMA = DATABASE()
                  AND tc.TABLE_NAME = 'app_users'
                  AND tc.CONSTRAINT_TYPE = 'FOREIGN KEY'
                  AND kcu.REFERENCED_TABLE_NAME = 'user_roles'
                """);

        for (Map<String, Object> row : constraints) {
            Object constraintNameObj = row.get("CONSTRAINT_NAME");
            if (constraintNameObj == null) {
                continue;
            }
            String constraintName = constraintNameObj.toString().replace("`", "``");
            jdbcTemplate.execute("ALTER TABLE app_users DROP FOREIGN KEY `" + constraintName + "`");
        }
    }
}
