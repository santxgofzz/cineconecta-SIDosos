package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.error.ApiException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

/** respuestas html para errores del modulo, sin afectar las api rest. */
@ControllerAdvice(assignableTypes = BillingPageController.class)
public class BillingPageErrors {
    @ExceptionHandler(ApiException.class)
    ModelAndView business(ApiException ex) {
        return page(ex.status(), ex.getMessage());
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ModelAndView databaseUnavailable() {
        return page(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pueden consultar las suscripciones ahora. Vuelve a intentar mas tarde.");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ModelAndView badRequest() {
        return page(HttpStatus.BAD_REQUEST, "Revisa los datos de la solicitud.");
    }

    private static ModelAndView page(HttpStatus status, String message) {
        ModelAndView view = new ModelAndView("subscription/error");
        view.setStatus(status);
        view.addObject("message", message);
        return view;
    }
}
