package co.edu.icesi.cineconecta.account;

import co.edu.icesi.cineconecta.persistence.oracle.entity.AccountEntity;

import java.time.Instant;

/** Representación pública de una cuenta sin exponer passwordHash. */
public record AccountResponse(Long accountId, String fullName, String email, Instant createdAt) {
    public static AccountResponse from(AccountEntity entity) {
        return new AccountResponse(
                entity.getAccountId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getCreatedAt()
        );
    }
}
