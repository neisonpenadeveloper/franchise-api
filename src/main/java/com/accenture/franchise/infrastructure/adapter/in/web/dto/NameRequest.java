package com.accenture.franchise.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo para crear o renombrar una franquicia o una sucursal: solo un nombre.
 *
 * <p>Se comparte entre esos endpoints porque el contrato es identico; si alguno
 * necesitara campos propios, se separa entonces y no antes.</p>
 */
@Schema(description = "Nombre de la franquicia o sucursal")
public record NameRequest(

        @Schema(description = "Nombre", example = "Franquicia Centro")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name) {
}
