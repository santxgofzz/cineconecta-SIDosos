package co.edu.icesi.cineconecta.account;

import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST para registro de clientes y consulta de la cuenta autenticada. */
@RestController
public class AccountController {
    private final AccountService accountService;
    private final CurrentAccount currentAccount;

    public AccountController(AccountService accountService, CurrentAccount currentAccount) {
        this.accountService = accountService;
        this.currentAccount = currentAccount;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody AccountRegistrationRequest request) {
        AccountResponse created = accountService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/accounts/me")
    public AccountResponse me() {
        return accountService.getAccount(currentAccount.requireAccountId());
    }
}
