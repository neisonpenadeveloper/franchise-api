package com.accenture.franchise.infrastructure.adapter.in.web.mapper;

import com.accenture.franchise.domain.model.Branch;
import com.accenture.franchise.domain.model.BranchTopProduct;
import com.accenture.franchise.domain.model.Franchise;
import com.accenture.franchise.domain.model.Product;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.BranchResponse;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.BranchTopProductResponse;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.FranchiseResponse;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.ProductResponse;

/**
 * Traduce el modelo de dominio a los DTO de respuesta.
 *
 * <p>El contrato HTTP queda desacoplado del modelo: cambiar el dominio no
 * rompe a los clientes de la API mientras el mapeo se mantenga.</p>
 */
public final class FranchiseWebMapper {

    private FranchiseWebMapper() {
    }

    public static FranchiseResponse toResponse(Franchise franchise) {
        return new FranchiseResponse(
                franchise.id(),
                franchise.name(),
                franchise.branches().stream().map(FranchiseWebMapper::toResponse).toList());
    }

    public static BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.id(),
                branch.name(),
                branch.products().stream().map(FranchiseWebMapper::toResponse).toList());
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.id(), product.name(), product.stock());
    }

    public static BranchTopProductResponse toResponse(BranchTopProduct topProduct) {
        return new BranchTopProductResponse(
                topProduct.branchId(),
                topProduct.branchName(),
                toResponse(topProduct.product()));
    }
}
