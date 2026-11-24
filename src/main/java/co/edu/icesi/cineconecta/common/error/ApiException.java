package co.edu.icesi.cineconecta.common.error;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio o de disponibilidad con código estable para la interfaz y
 * las pruebas. Nunca debe usarse para informar un éxito no confirmado.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }

    public static ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException unavailable(String code, String message, Throwable cause) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message, cause);
    }
}
