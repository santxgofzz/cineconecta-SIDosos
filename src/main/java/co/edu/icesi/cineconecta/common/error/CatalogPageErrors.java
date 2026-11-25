package co.edu.icesi.cineconecta.common.error;

import co.edu.icesi.cineconecta.admin.AdminCatalogPageController;
import co.edu.icesi.cineconecta.catalog.CatalogPageController;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = {CatalogPageController.class, AdminCatalogPageController.class})
public class CatalogPageErrors {
    @ExceptionHandler(ApiException.class)
    ModelAndView businessError(ApiException ex) {
        return view(ex.status(), ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    ModelAndView unavailable() {
        return view(HttpStatus.SERVICE_UNAVAILABLE, "El catálogo no está disponible temporalmente.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ModelAndView invalidParameters() {
        return view(HttpStatus.BAD_REQUEST, "Revisa los parámetros de la solicitud.");
    }

    private static ModelAndView view(HttpStatus status, String message) {
        var result = new ModelAndView("catalog/error");
        result.setStatus(status);
        result.addObject("message", message);
        return result;
    }
}
