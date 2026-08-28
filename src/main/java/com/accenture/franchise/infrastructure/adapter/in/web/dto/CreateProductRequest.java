package com.accenture.franchise.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Cuerpo para agregar un producto a una sucursal. */
@Schema(description = "Producto a agregar en la sucursal")
public record CreateProductRequest(

        @Schema(description = "Nombre del producto", example = "Cafe 500g")
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name,

        @Schema(description = "Unidades disponibles", example = "120")
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock) {
}
