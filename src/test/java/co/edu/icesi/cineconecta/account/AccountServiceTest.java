package co.edu.icesi.cineconecta.account;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.common.security.AccountPrincipal;
import co.edu.icesi.cineconecta.persistence.oracle.entity.AccountEntity;
import co.edu.icesi.cineconecta.persistence.oracle.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-24T18:00:00Z");

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AccountService service = new AccountService(accounts, encoder, clock);
    private final AccountUserDetailsService userDetailsService = new AccountUserDetailsService(accounts);

    @Test
    void registersAccountWithBcryptHashAndNormalizedEmail() {
        when(accounts.existsByEmailIgnoreCase("ana@ejemplo.com")).thenReturn(false);
        when(accounts.saveAndFlush(any(AccountEntity.class))).thenAnswer(inv -> {
            AccountEntity e = inv.getArgument(0);
            return new AccountEntity(101L, e.getFullName(), e.getEmail(), e.getPasswordHash(), e.getCreatedAt());
        });

        var res = service.register(new AccountRegistrationRequest(
                "  Ana Pérez  ", "  ANA@Ejemplo.com ", "ClaveSegura2026!"));

        assertEquals(101L, res.accountId());
        assertEquals("Ana Pérez", res.fullName());
        assertEquals("ana@ejemplo.com", res.email());
        assertEquals(NOW, res.createdAt());

        var captor = org.mockito.ArgumentCaptor.forClass(AccountEntity.class);
        verify(accounts).saveAndFlush(captor.capture());
        String storedHash = captor.getValue().getPasswordHash();
        assertNotEquals("ClaveSegura2026!", storedHash);
        assertTrue(storedHash.startsWith("$2"));
        assertTrue(encoder.matches("ClaveSegura2026!", storedHash));
    }

    @Test
    void rejectsDuplicateEmailCaseInsensitively() {
        when(accounts.existsByEmailIgnoreCase("ana@ejemplo.com")).thenReturn(true);

        var ex = assertThrows(ApiException.class, () -> service.register(
                new AccountRegistrationRequest("Ana", "ANA@ejemplo.com", "ClaveSegura2026!")));

        assertEquals(HttpStatus.CONFLICT, ex.status());
        assertEquals(ErrorCodes.EMAIL_ALREADY_EXISTS, ex.code());
    }

    @Test
    void translatesDatabaseUniqueViolationOnConcurrentRegistration() {
        when(accounts.existsByEmailIgnoreCase("ana@ejemplo.com")).thenReturn(false);
        when(accounts.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("ACCOUNTS_EMAIL_LOWER_UK"));

        var ex = assertThrows(ApiException.class, () -> service.register(
                new AccountRegistrationRequest("Ana", "ana@ejemplo.com", "ClaveSegura2026!")));

        assertEquals(HttpStatus.CONFLICT, ex.status());
        assertEquals(ErrorCodes.EMAIL_ALREADY_EXISTS, ex.code());
    }

    @Test
    void userDetailsServiceLoadsAccountPrincipalWithCustomerRole() {
        String hash = encoder.encode("ClaveSegura2026!");
        when(accounts.findByEmailIgnoreCase("ana@ejemplo.com"))
                .thenReturn(Optional.of(new AccountEntity(42L, "Ana", "ana@ejemplo.com", hash, NOW)));

        var details = userDetailsService.loadUserByUsername("  ANA@ejemplo.com ");

        AccountPrincipal principal = assertInstanceOf(AccountPrincipal.class, details);
        assertEquals(42L, principal.getAccountId());
        assertEquals("ana@ejemplo.com", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    void userDetailsServiceRejectsUnknownOrBlankEmail() {
        when(accounts.findByEmailIgnoreCase("noexiste@ejemplo.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(" "));
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("noexiste@ejemplo.com"));
    }
}
