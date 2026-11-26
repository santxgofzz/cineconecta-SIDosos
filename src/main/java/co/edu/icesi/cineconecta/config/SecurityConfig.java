package co.edu.icesi.cineconecta.config;

import co.edu.icesi.cineconecta.account.AccountUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para titulares de cuentas (Oracle).
 * Los administradores (MongoDB) están aislados en {@link AdminSecurityConfig} (@Order(1)).
 */
@Configuration
public class SecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ObjectProvider<AccountUserDetailsService> accountUsersProvider,
                                            ObjectProvider<UserDetailsService> fallbackUsersProvider,
                                            PasswordEncoder encoder) throws Exception {
        UserDetailsService accountUsers = accountUsersProvider.getIfAvailable();
        if (accountUsers == null) {
            accountUsers = fallbackUsersProvider.orderedStream().findFirst().orElse(null);
        }
        if (accountUsers != null) {
            var provider = new DaoAuthenticationProvider(accountUsers);
            provider.setPasswordEncoder(encoder);
            http.authenticationManager(new ProviderManager(provider));
        }

        var redirect = new DefaultRedirectStrategy();
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/error",
                                "/login", "/register", "/reportes/ingresos-por-plan").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            if (wantsHtml(request)) {
                                redirect.sendRedirect(request, response, "/");
                            } else {
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                            }
                        })
                        .failureHandler((request, response, failure) -> {
                            if (wantsHtml(request)) {
                                redirect.sendRedirect(request, response, "/login?error");
                            } else {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if (wantsHtml(request)) {
                                redirect.sendRedirect(request, response, "/login?logout");
                            } else {
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                            }
                        }))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static boolean wantsHtml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }
}
