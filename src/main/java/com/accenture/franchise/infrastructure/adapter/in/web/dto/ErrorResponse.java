package com.accenture.franchise.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Formato unico de error de la API, para que el cliente siempre reciba la misma
 * estructura sea cual sea el fallo.
 *
 * @param status  codigo HTTP
 * @param error   etiqueta corta y estable del tipo de error
 * @param message descripcion legible
 * @param details errores de validacion campo a campo; ausente si no aplica
 */
@Schema(description = "Respuesta de error de la API")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String error,
        String message,
        List<String> details,
        Instant timestamp) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, List.of(), Instant.now());
    }

    public static ErrorResponse of(int status, String error, String message, List<String> details) {
        return new ErrorResponse(status, error, message, details, Instant.now());
    }
}
