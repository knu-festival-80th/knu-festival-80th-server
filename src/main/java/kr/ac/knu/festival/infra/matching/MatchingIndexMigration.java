package kr.ac.knu.festival.infra.matching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingIndexMigration implements CommandLineRunner {

    private static final String[] OLD_INDEXES = {
            "uk_matching_instagram_id",
            "uk_matching_phone_lookup_hash",
            "uk_matching_participant_id_day"
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        for (String indexName : OLD_INDEXES) {
            try {
                int count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND table_name = 'matching_participant' AND index_name = ?",
                        Integer.class, indexName);
                if (count > 0) {
                    jdbcTemplate.execute("ALTER TABLE matching_participant DROP INDEX " + indexName);
                    log.info("Dropped old index: {}", indexName);
                }
            } catch (Exception e) {
                log.debug("Skipping index migration for {}: {}", indexName, e.getMessage());
            }
        }
    }
}
