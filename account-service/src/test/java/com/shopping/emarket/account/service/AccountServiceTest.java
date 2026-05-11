package com.shopping.emarket.account.service;

import com.shopping.emarket.account.domain.User;
import com.shopping.emarket.account.dto.AddressDto;
import com.shopping.emarket.account.dto.CreateAccountRequest;
import com.shopping.emarket.account.dto.UpdateAccountRequest;
import com.shopping.emarket.account.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountServiceTest {

    private UserRepository repo;
    private PasswordEncoder encoder;
    private AccountService service;

    @BeforeEach
    void setup() {
        repo = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new AccountService(repo, encoder);
    }

    @Test
    void registerPersistsBCryptedPassword() {
        when(repo.existsByEmail("a@b.c")).thenReturn(false);
        when(repo.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.register(new CreateAccountRequest(
                "a@b.c", "alice", "Passw0rd!",
                new AddressDto("1 A St", null, "LA", "CA", "90001", "US"),
                null));

        assertThat(resp.email()).isEqualTo("a@b.c");
        assertThat(resp.shippingAddress().city()).isEqualTo("LA");
        assertThat(resp.id()).isNotNull();
    }

    @Test
    void registerRejectsDuplicateEmailPreflight() {
        when(repo.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new CreateAccountRequest(
                "dup@example.com", "alice", "Passw0rd!", null, null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateAppliesChangesInPlace() {
        UUID id = UUID.randomUUID();
        User existing = new User(id, "a@b.c", "alice", encoder.encode("Passw0rd!"));
        when(repo.findById(id)).thenReturn(Optional.of(existing));

        var resp = service.update(id, new UpdateAccountRequest(
                "alice2",
                new AddressDto("2 B St", null, "SF", "CA", "94001", "US"),
                null));

        assertThat(resp.username()).isEqualTo("alice2");
        assertThat(resp.shippingAddress().city()).isEqualTo("SF");
    }
}
