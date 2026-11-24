package co.edu.icesi.cineconecta.config;

import co.edu.icesi.cineconecta.persistence.mongo.document.AdminUserDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.AdminUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class AdminSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain adminSecurity(HttpSecurity http, AdminUserRepository users,
                                      PasswordEncoder encoder) throws Exception {
        var provider = new DaoAuthenticationProvider(username -> {
            String normalized = AdminUserDocument.normalizeUsername(username);
            var admin = users.findByUsernameNormalized(normalized)
                    .filter(user -> "ADMIN".equals(user.role()))
                    .orElseThrow(() -> new UsernameNotFoundException("Administrador no encontrado."));
            return User.withUsername(admin.usernameNormalized())
                    .password(admin.passwordHash())
                    .roles("ADMIN").disabled(!admin.active()).build();
        });
        provider.setPasswordEncoder(encoder);

        var adminContext = new HttpSessionSecurityContextRepository();
        adminContext.setSpringSecurityContextKey("CINECONECTA_ADMIN_SECURITY_CONTEXT");
        return http.securityMatcher("/admin/**", "/api/admin/**")
                .securityContext(context -> context.securityContextRepository(adminContext))
                .authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/admin/**")))
                .formLogin(form -> form.loginPage("/admin/login").loginProcessingUrl("/admin/login")
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, failure) -> response.setStatus(401)))
                .logout(logout -> logout.logoutUrl("/admin/logout").invalidateHttpSession(false)
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .build();
    }
}
