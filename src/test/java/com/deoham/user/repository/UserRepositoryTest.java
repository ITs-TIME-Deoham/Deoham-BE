package com.deoham.user.repository;

import com.deoham.TestcontainersConfiguration;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@EnableJpaAuditing
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindByStatusAndDeletedAtBefore_EnumCastingWorks() {
        // Verifies the fix for: "operator does not exist: user_status = character varying"
        // by confirming the native query with ?1::user_status casting executes without error
        Instant cutoffTime = Instant.now().minus(30, ChronoUnit.DAYS);

        // Act & Assert: Query should execute successfully without throwing
        // org.postgresql.util.PSQLException about operator mismatch
        List<User> result = userRepository.findByStatusAndDeletedAtBefore(
            UserStatus.DELETED.name(),
            cutoffTime);

        // The query executed without casting errors (no exception thrown)
        assertThat(result).isNotNull();
    }
}
