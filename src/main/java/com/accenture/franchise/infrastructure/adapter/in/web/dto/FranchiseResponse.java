package com.accenture.franchise.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Representacion de una franquicia con sus sucursales y productos. */
@Schema(description = "Franquicia con sus sucursales y productos")
public record FranchiseResponse(String id, String name, List<BranchResponse> branches) {
}
