package co.edu.icesi.cineconecta.config;

import co.edu.icesi.cineconecta.common.security.AccountPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;

/**
 * SOLO perfil {@code dev}: titular de prueba para usar la interfaz simulada
 * mientras el módulo account implementa la autenticación real contra Oracle.
 * La cuenta debe existir en Oracle. Sin valores por defecto para la clave.
 *
 * <pre>
 * SPRING_PROFILES_ACTIVE=dev
 * DEV_ACCOUNT_ID=1  DEV_USERNAME=titular@demo  DEV_PASSWORD=...
 * </pre>
 */
@Configuration
@Profile("dev")
public class DevAccountUserConfig {

    @Bean
    UserDetailsService devAccountUsers(PasswordEncoder encoder,
                                       @Value("${DEV_USERNAME}") String username,
                                       @Value("${DEV_PASSWORD}") String password,
                                       @Value("${DEV_ACCOUNT_ID}") long accountId) {
        UserDetails user = new DevAccountUser(username, encoder.encode(password), accountId);
        return name -> {
            if (user.getUsername().equals(name)) {
                return new DevAccountUser(user.getUsername(), user.getPassword(), accountId);
            }
            throw new UsernameNotFoundException(name);
        };
    }

    static final class DevAccountUser extends User implements AccountPrincipal {
        private static final Collection<GrantedAuthority> ROLES = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        private final long accountId;

        DevAccountUser(String username, String passwordHash, long accountId) {
            super(username, passwordHash, ROLES);
            this.accountId = accountId;
        }

        @Override
        public long getAccountId() {
            return accountId;
        }
    }
}
