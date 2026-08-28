package com.accenture.franchise.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Producto con mas stock de una sucursal, indicando a que sucursal pertenece.
 * Es la respuesta del criterio 7 de la prueba.
 */
@Schema(description = "Producto con mas stock de una sucursal")
public record BranchTopProductResponse(String branchId, String branchName, ProductResponse product) {
}
