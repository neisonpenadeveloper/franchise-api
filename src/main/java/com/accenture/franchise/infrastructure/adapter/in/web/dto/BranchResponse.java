package com.accenture.franchise.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Representacion de una sucursal en las respuestas. */
@Schema(description = "Sucursal de una franquicia")
public record BranchResponse(String id, String name, List<ProductResponse> products) {
}
