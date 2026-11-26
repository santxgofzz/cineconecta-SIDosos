package co.edu.icesi.cineconecta.account;

import co.edu.icesi.cineconecta.common.security.AccountPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Principal autenticado de un titular de cuenta en Oracle. */
public class AccountUserDetails implements UserDetails, AccountPrincipal {
    private static final Collection<GrantedAuthority> ROLES =
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

    private final long accountId;
    private final String email;
    private final String passwordHash;

    public AccountUserDetails(long accountId, String email, String passwordHash) {
        this.accountId = accountId;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return ROLES;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
