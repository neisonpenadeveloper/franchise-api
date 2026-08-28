package com.accenture.franchise.domain.model;

/**
 * Resultado de la consulta "producto con mas stock por sucursal": el producto
 * junto con la sucursal a la que pertenece.
 */
public record BranchTopProduct(String branchId, String branchName, Product product) {
}
