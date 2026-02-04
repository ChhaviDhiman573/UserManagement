package com.wellness.mockitoTest;

import com.wellness.data.Role;
import com.wellness.data.Status;
import com.wellness.data.Users;
import com.wellness.repository.IUserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// 👉 As you requested, keeping this import unchanged:
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")

// ✅ Do NOT auto-replace our DataSource with a random in-memory one
@AutoConfigureTestDatabase(replace = Replace.NONE)

// ✅ Force H2 to run in MySQL compatibility mode for this test slice
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",

        // Keep MySQL dialect; H2 in MySQL MODE will accept MySQL DDL.
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",

        // Ensure schema is created and dropped for each test run
        "spring.jpa.hibernate.ddl-auto=create-drop",

        // Avoid running data.sql during tests
        "spring.sql.init.mode=never"
})
class RepositoryTest {

    @Autowired
    private IUserRepository repository;

    private Users buildUser(
            String name,
            String email,
            String password,
            String department,
            Integer managerId,
            Role role,
            Status status
    ) {
        Users u = new Users();
        u.setUserId(null); // generated
        u.setName(name);
        u.setEmail(email);
        u.setPassword(password);
        u.setDepartment(department);
        u.setManagerId(managerId); // nullable if schema allows
        u.setRole(role);
        u.setStatus(status);
        return u;
    }

    @Test
    @DisplayName("save: assigns ID and sets createdAt via @CreationTimestamp")
    void save_assignsId_and_setsCreatedAt() {
        Users toSave = buildUser(
                "Alice", "alice@example.com", "secret", "Engineering", 1001,
                Role.EMPLOYEE, Status.ACTIVE
        );

        Users saved = repository.save(toSave);

        assertThat(saved.getUserId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull()
                .isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("findById: returns existing entity; empty for unknown id")
    void findById_works() {
        Users saved = repository.save(buildUser(
                "Bob", "bob@example.com", "p@ss", "Finance", null, Role.MANAGER, Status.ACTIVE
        ));

        Optional<Users> found = repository.findById(saved.getUserId());
        Optional<Users> notFound = repository.findById(-999);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("bob@example.com");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("findAll: returns all persisted users")
    void findAll_returnsAll() {
        repository.save(buildUser("U1", "u1@example.com", "x", "Dept1", null, Role.EMPLOYEE, Status.ACTIVE));
        repository.save(buildUser("U2", "u2@example.com", "y", "Dept2", 42, Role.MANAGER, Status.INACTIVE));

        List<Users> all = repository.findAll();

        assertThat(all).hasSize(2);
        assertThat(all)
                .extracting(Users::getEmail)
                .containsExactlyInAnyOrder("u1@example.com", "u2@example.com");
    }

    @Nested
    class CustomQueryMethods {

        @Test
        @DisplayName("findByEmail: returns matching user; null when not found")
        void findByEmail_works() {
            repository.save(buildUser("Cathy", "cathy@example.com", "z", "Ops", null, Role.EMPLOYEE, Status.ACTIVE));

            Users found = repository.findByEmail("cathy@example.com");
            Users notFound = repository.findByEmail("nobody@example.com");

            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("Cathy");
            assertThat(notFound).isNull();
        }

        @Test
        @DisplayName("existsByEmail: true when present; false otherwise")
        void existsByEmail_works() {
            repository.save(buildUser("Dan", "dan@example.com", "pwd", "Ops", null, Role.MANAGER, Status.INACTIVE));

            assertThat(repository.existsByEmail("dan@example.com")).isTrue();
            assertThat(repository.existsByEmail("ghost@example.com")).isFalse();
        }
    }

    @Nested
    class ConstraintsAndEnums {

        @Test
        @DisplayName("Non-null constraints: name/email/password/department/role/status must be present")
        void nonNullConstraints_enforced() {
            Users invalid = new Users(); // missing required fields

            // ✅ Because @PrePersist throws IllegalArgumentException, Spring wraps it as InvalidDataAccessApiUsageException
            assertThatThrownBy(() -> repository.saveAndFlush(invalid))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class)
                    .hasMessageContaining("Email or password cannot be null");
        }

        @Test
        @DisplayName("Enum persistence: Role and Status are stored and retrieved correctly")
        void enumPersistence_works() {
            Users saved = repository.save(buildUser(
                    "Eva", "eva@example.com", "pwd", "QA", null, Role.EMPLOYEE, Status.ACTIVE
            ));

            Users reloaded = repository.findById(saved.getUserId()).orElseThrow();
            assertThat(reloaded.getRole()).isEqualTo(Role.EMPLOYEE);
            assertThat(reloaded.getStatus()).isEqualTo(Status.ACTIVE);
        }
    }
}