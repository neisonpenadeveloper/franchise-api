package com.accenture.franchise.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Cuerpo para modificar el stock de un producto. */
@Schema(description = "Nuevo stock del producto")
public record UpdateStockRequest(

        @Schema(description = "Unidades disponibles", example = "35")
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock) {
}
