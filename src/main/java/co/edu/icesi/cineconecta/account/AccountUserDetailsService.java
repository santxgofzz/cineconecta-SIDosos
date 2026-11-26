package co.edu.icesi.cineconecta.account;

import co.edu.icesi.cineconecta.persistence.oracle.repository.AccountRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Carga titulares de cuenta desde Oracle para la autenticación de Spring Security. */
@Service
@Profile("!dev")
public class AccountUserDetailsService implements UserDetailsService {
    private final AccountRepository accounts;

    public AccountUserDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Correo requerido.");
        }
        String normalized = normalizeEmail(username);
        var account = accounts.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Cuenta no encontrada."));
        return new AccountUserDetails(account.getAccountId(), account.getEmail(), account.getPasswordHash());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
