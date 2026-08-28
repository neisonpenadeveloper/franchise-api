package com.accenture.franchise.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Representacion de un producto en las respuestas. */
@Schema(description = "Producto ofertado en una sucursal")
public record ProductResponse(String id, String name, int stock) {
}
