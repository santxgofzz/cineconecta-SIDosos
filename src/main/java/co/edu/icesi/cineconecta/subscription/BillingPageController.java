package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseRequest;
import co.edu.icesi.cineconecta.subscription.BillingModels.SubscriptionHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

/** interfaz de contratacion con formularios normales y proteccion csrf. */
@Controller
@RequestMapping("/subscriptions")
public class BillingPageController {
    private static final Logger log = LoggerFactory.getLogger(BillingPageController.class);
    private final BillingService billing;
    private final CurrentAccount currentAccount;

    public BillingPageController(BillingService billing, CurrentAccount currentAccount) {
        this.billing = billing;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public String index(Model model) {
        long accountId = currentAccount.requireAccountId();
        var plans = billing.plans();
        List<SubscriptionHistory> history = billing.history(accountId);

        model.addAttribute("plans", plans);
        model.addAttribute("history", history);
        model.addAttribute("activeSubscription", history.stream()
                .filter(SubscriptionHistory::active)
                .max(Comparator.comparing(SubscriptionHistory::startsAt))
                .orElse(null));
        return "subscription/index";
    }

    @PostMapping("/purchase")
    public String purchase(@RequestParam(required = false) Long planId,
                           @RequestParam(required = false) Boolean approvePayment,
                           RedirectAttributes flash) {
        // el id del titular siempre viene de la sesion, nunca del formulario.
        long accountId = currentAccount.requireAccountId();
        try {
            var receipt = billing.purchase(accountId, new PurchaseRequest(planId, approvePayment));
            flash.addFlashAttribute("receipt", receipt);
            flash.addFlashAttribute("notice", "La suscripcion se registro y el pago simulado fue aprobado.");
        } catch (ApiException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException | TransactionException ex) {
            // no mostramos consultas ni detalles internos de oracle en la pagina.
            log.warn("No se pudo confirmar la compra en oracle", ex);
            flash.addFlashAttribute("error", "No se pudo confirmar la compra. Verifica la conexion e intenta de nuevo.");
        }
        // redirect evita que actualizar la pagina repita una compra.
        return "redirect:/subscriptions";
    }
}
