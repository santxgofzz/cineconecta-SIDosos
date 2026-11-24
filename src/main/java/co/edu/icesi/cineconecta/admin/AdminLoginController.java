package co.edu.icesi.cineconecta.admin;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminLoginController {
    public record LoginCsrf(String header, String parameter, String token) {}

    @GetMapping("/admin/login")
    public ResponseEntity<LoginCsrf> csrf(CsrfToken csrf) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new LoginCsrf(csrf.getHeaderName(), csrf.getParameterName(), csrf.getToken()));
    }
}
