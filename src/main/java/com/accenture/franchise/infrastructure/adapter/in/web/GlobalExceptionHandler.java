package com.accenture.franchise.infrastructure.adapter.in.web;

import com.accenture.franchise.domain.exception.ConcurrentUpdateException;
import com.accenture.franchise.domain.exception.DuplicateNameException;
import com.accenture.franchise.domain.exception.InvalidDataException;
import com.accenture.franchise.domain.exception.NotFoundException;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;

/**
 * Traduce las excepciones a respuestas HTTP con un formato unico.
 *
 * <p>Es el unico punto donde el dominio se convierte en codigos HTTP: asi las
 * capas internas lanzan errores de negocio sin saber nada del protocolo.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateName(DuplicateNameException exception) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_NAME", exception.getMessage());
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidData(InvalidDataException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_DATA", exception.getMessage());
    }

    /**
     * Dos escrituras simultaneas chocaron sobre la misma franquicia.
     *
     * <p>El caso de uso ya reintenta la operacion; que llegue hasta aqui
     * significa que el conflicto persistio. Se responde 409 porque el estado del
     * recurso cambio bajo los pies de la peticion: reintentarla puede funcionar,
     * mientras que un 500 sugeriria una averia del servidor.</p>
     */
    @ExceptionHandler(ConcurrentUpdateException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate(ConcurrentUpdateException exception) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", exception.getMessage());
    }

    /** Falla la validacion de los DTO anotados con {@code @Valid}. */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException exception) {
        List<String> details = exception.getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                        "La peticion tiene campos invalidos", details));
    }

    /** JSON mal formado o tipo de dato incorrecto en el cuerpo o la ruta. */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleBadInput(ServerWebInputException exception) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "La peticion no se pudo interpretar");
    }

    /**
     * Choque contra el indice unico de nombre de franquicia.
     *
     * <p>Se comprueba antes en el caso de uso, pero dos peticiones simultaneas
     * pueden pasar esa comprobacion a la vez; el indice de Mongo es la garantia
     * real y aqui se traduce a 409 en lugar de a un 500.</p>
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException exception) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_NAME", "Ya existe una franquicia con ese nombre");
    }

    /**
     * Errores que Spring ya emite con un codigo HTTP propio, como una peticion a
     * una ruta que no existe.
     *
     * <p>Sin este manejador el catch-all de abajo los convertiria en 500, y una
     * URL equivocada se reportaria como una averia del servidor.</p>
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();
        return build(status, status.name(), message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Error no controlado", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno del servidor");
    }

    private static String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), code, message));
    }
}
