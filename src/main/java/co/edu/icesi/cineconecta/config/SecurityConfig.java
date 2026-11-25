package co.edu.icesi.cineconecta.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración TEMPORAL del esqueleto. No sustituye la autenticación de
 * clientes (Oracle) y administradores (MongoDB). Implementar ambas.
 */
@Configuration
public class SecurityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/error",
                            "/reportes/ingresos-por-plan").permitAll()
                    .anyRequest().authenticated())
                .formLogin(form -> form.permitAll())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(SecurityProperties securityProperties,
                                          PasswordEncoder passwordEncoder) {
        var configuredUser = securityProperties.getUser();
        if (configuredUser.isPasswordGenerated()) {
            LOGGER.warn("Using generated security password: {}", configuredUser.getPassword());
        }

        var user = User.withUsername(configuredUser.getName())
                .password(passwordEncoder.encode(configuredUser.getPassword()))
                .roles(configuredUser.getRoles().toArray(String[]::new))
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
