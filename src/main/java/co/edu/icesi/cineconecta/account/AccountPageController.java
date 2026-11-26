package co.edu.icesi.cineconecta.account;

import co.edu.icesi.cineconecta.common.error.ApiException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

/** Páginas MVC de inicio de sesión y registro de titulares de cuenta. */
@Controller
public class AccountPageController {
    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final AccountService accountService;

    public AccountPageController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/login")
    public String login() {
        return "account/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("fullName", "");
        model.addAttribute("email", "");
        return "account/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam(defaultValue = "") String fullName,
                           @RequestParam(defaultValue = "") String email,
                           @RequestParam(defaultValue = "") String password,
                           @RequestParam(defaultValue = "") String confirmPassword,
                           Model model) {
        String trimmedName = fullName.trim();
        String trimmedEmail = email.trim();
        model.addAttribute("fullName", trimmedName);
        model.addAttribute("email", trimmedEmail);

        String validationError = validate(trimmedName, trimmedEmail, password, confirmPassword);
        if (validationError != null) {
            model.addAttribute("errorMessage", validationError);
            return "account/register";
        }

        try {
            accountService.register(new AccountRegistrationRequest(trimmedName, trimmedEmail, password));
            return "redirect:/login?registered";
        } catch (ApiException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "account/register";
        }
    }

    private static String validate(String fullName, String email, String password, String confirmPassword) {
        if (fullName.isEmpty() || fullName.length() > 150) {
            return "Ingresa un nombre completo válido (máximo 150 caracteres).";
        }
        if (email.isEmpty() || email.length() > 254 || !SIMPLE_EMAIL.matcher(email).matches()) {
            return "Ingresa un correo electrónico válido.";
        }
        if (password.isBlank() || password.length() < 8 || password.length() > 72) {
            return "La contraseña debe tener entre 8 y 72 caracteres.";
        }
        if (!password.equals(confirmPassword)) {
            return "Las contraseñas no coinciden.";
        }
        return null;
    }
}
