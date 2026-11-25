package co.edu.icesi.cineconecta.account;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.persistence.oracle.entity.AccountEntity;
import co.edu.icesi.cineconecta.persistence.oracle.repository.AccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/** Registro y consulta de titulares de cuenta en Oracle. */
@Service
public class AccountService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder, Clock clock) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse register(AccountRegistrationRequest request) {
        String fullName = request.fullName().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (fullName.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.INVALID_INPUT, "El nombre completo es obligatorio.");
        }
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict(ErrorCodes.EMAIL_ALREADY_EXISTS,
                    "Ya existe una cuenta registrada con ese correo.");
        }
        String hash = passwordEncoder.encode(request.password());
        var entity = new AccountEntity(fullName, email, hash, Instant.now(clock));
        try {
            var saved = accounts.saveAndFlush(entity);
            return AccountResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict(ErrorCodes.EMAIL_ALREADY_EXISTS,
                    "Ya existe una cuenta registrada con ese correo.");
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(long accountId) {
        var account = accounts.findById(accountId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.ACCOUNT_NOT_FOUND, "Cuenta no encontrada."));
        return AccountResponse.from(account);
    }
}
