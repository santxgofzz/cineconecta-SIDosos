package co.edu.icesi.cineconecta.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/** Traduce errores de los controladores REST a respuestas JSON uniformes. */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record ErrorBody(String code, String message, Instant timestamp) {}

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> handle(ApiException ex) {
        if (ex.status().is5xxServerError()) {
            log.warn("{}: {}", ex.code(), ex.getMessage(), ex.getCause());
        }
        return ResponseEntity.status(ex.status()).body(new ErrorBody(ex.code(), ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> handle(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorBody(ErrorCodes.INVALID_EVENT, "Solicitud inválida", Instant.now()));
    }

    /** Último recurso: un fallo de BD no capturado nunca se informa como éxito. */
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorBody> handle(DataAccessException ex) {
        log.error("Fallo de acceso a datos no controlado", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorBody("DATABASE_UNAVAILABLE",
                "No se pudo confirmar la operación. Intenta de nuevo más tarde.", Instant.now()));
    }
}
