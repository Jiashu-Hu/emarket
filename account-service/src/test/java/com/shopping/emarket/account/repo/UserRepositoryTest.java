package com.shopping.emarket.account.repo;

import com.shopping.emarket.account.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    UserRepository repo;

    @Test
    void savesAndFindsByEmail() {
        User saved = repo.save(new User(UUID.randomUUID(), "a@b.c", "alice", "hash"));

        Optional<User> found = repo.findByEmail("a@b.c");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        repo.save(new User(UUID.randomUUID(), "dup@example.com", "one", "hash"));
        repo.flush();

        assertThatThrownBy(() -> {
            repo.saveAndFlush(new User(UUID.randomUUID(), "dup@example.com", "two", "hash"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByEmailTrueAfterSave() {
        repo.save(new User(UUID.randomUUID(), "present@example.com", "alice", "hash"));

        assertThat(repo.existsByEmail("present@example.com")).isTrue();
        assertThat(repo.existsByEmail("missing@example.com")).isFalse();
    }
}
