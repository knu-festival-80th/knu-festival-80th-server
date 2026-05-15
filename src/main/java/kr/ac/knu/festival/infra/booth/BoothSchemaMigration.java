package kr.ac.knu.festival.infra.booth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoothSchemaMigration implements CommandLineRunner {

    private static final String TOTAL_WAITING_COUNT_COLUMN = "total_waiting_count";
    private static final String TOTAL_WAITING_COUNT_INDEX = "idx_booth_total_waiting_count";
    private static final int MYSQL_DUPLICATE_COLUMN_ERROR_CODE = 1060;
    private static final int MYSQL_DUPLICATE_KEY_ERROR_CODE = 1061;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        ensureTotalWaitingCountColumn();
        ensureTotalWaitingCountIndex();
    }

    private void ensureTotalWaitingCountColumn() {
        try {
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                            "WHERE table_schema = DATABASE() AND table_name = 'booth' AND column_name = ?",
                    Integer.class,
                    TOTAL_WAITING_COUNT_COLUMN);
            if (count == 0) {
                jdbcTemplate.execute("ALTER TABLE booth ADD COLUMN total_waiting_count INT NOT NULL DEFAULT 0");
                log.info("Added booth.{} column", TOTAL_WAITING_COUNT_COLUMN);
            }
        } catch (Exception e) {
            if (isMysqlError(e, MYSQL_DUPLICATE_COLUMN_ERROR_CODE)) {
                log.info("Booth column already exists, skipping migration: {}", TOTAL_WAITING_COUNT_COLUMN);
                return;
            }
            log.error("Failed to migrate booth column: {}", TOTAL_WAITING_COUNT_COLUMN, e);
            throw new IllegalStateException(
                    "Failed to migrate booth column: " + TOTAL_WAITING_COUNT_COLUMN, e);
        }
    }

    private void ensureTotalWaitingCountIndex() {
        try {
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics " +
                            "WHERE table_schema = DATABASE() AND table_name = 'booth' AND index_name = ?",
                    Integer.class,
                    TOTAL_WAITING_COUNT_INDEX);
            if (count == 0) {
                jdbcTemplate.execute("CREATE INDEX idx_booth_total_waiting_count ON booth (total_waiting_count)");
                log.info("Added booth index: {}", TOTAL_WAITING_COUNT_INDEX);
            }
        } catch (Exception e) {
            if (isMysqlError(e, MYSQL_DUPLICATE_KEY_ERROR_CODE)) {
                log.info("Booth index already exists, skipping migration: {}", TOTAL_WAITING_COUNT_INDEX);
                return;
            }
            log.error("Failed to migrate booth index: {}", TOTAL_WAITING_COUNT_INDEX, e);
            throw new IllegalStateException(
                    "Failed to migrate booth index: " + TOTAL_WAITING_COUNT_INDEX, e);
        }
    }

    private boolean isMysqlError(Throwable throwable, int errorCode) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() == errorCode) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
