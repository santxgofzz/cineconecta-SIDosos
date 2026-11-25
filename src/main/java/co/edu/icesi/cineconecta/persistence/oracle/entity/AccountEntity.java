package co.edu.icesi.cineconecta.persistence.oracle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;

/** Titular de cuenta almacenado en la tabla Oracle ACCOUNTS. */
@Entity
@Table(name = "ACCOUNTS")
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_accounts")
    @SequenceGenerator(name = "seq_accounts", sequenceName = "SEQ_ACCOUNTS", allocationSize = 1)
    @Column(name = "ACCOUNTID", nullable = false)
    private Long accountId;

    @Column(name = "FULLNAME", nullable = false, length = 150)
    private String fullName;

    @Column(name = "EMAIL", nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "PASSWORDHASH", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "CREATEDAT", nullable = false)
    private Instant createdAt;

    protected AccountEntity() {}

    public AccountEntity(Long accountId, String fullName, String email, String passwordHash, Instant createdAt) {
        this.accountId = accountId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public AccountEntity(String fullName, String email, String passwordHash, Instant createdAt) {
        this(null, fullName, email, passwordHash, createdAt);
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
