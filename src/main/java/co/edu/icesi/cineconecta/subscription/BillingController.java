package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.subscription.BillingModels.Plan;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseReceipt;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseRequest;
import co.edu.icesi.cineconecta.subscription.BillingModels.SubscriptionHistory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** la cuenta sale del login, nunca de un id enviado por el navegador. */
@RestController
@RequestMapping("/api/subscriptions")
public class BillingController {
    private final BillingService billing;
    private final CurrentAccount currentAccount;

    public BillingController(BillingService billing, CurrentAccount currentAccount) {
        this.billing = billing;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/plans")
    public List<Plan> plans() {
        currentAccount.requireAccountId();
        return billing.plans();
    }

    @GetMapping("/history")
    public List<SubscriptionHistory> history() {
        return billing.history(currentAccount.requireAccountId());
    }

    @PostMapping("/purchase")
    public ResponseEntity<PurchaseReceipt> purchase(@Valid @RequestBody PurchaseRequest request) {
        long accountId = currentAccount.requireAccountId();
        return ResponseEntity.status(HttpStatus.CREATED).body(billing.purchase(accountId, request));
    }
}
