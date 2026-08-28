package com.accenture.franchise.infrastructure.adapter.out.mongo.mapper;

import com.accenture.franchise.domain.model.Branch;
import com.accenture.franchise.domain.model.Franchise;
import com.accenture.franchise.domain.model.Product;
import com.accenture.franchise.infrastructure.adapter.out.mongo.document.BranchDocument;
import com.accenture.franchise.infrastructure.adapter.out.mongo.document.FranchiseDocument;
import com.accenture.franchise.infrastructure.adapter.out.mongo.document.ProductDocument;

import java.util.List;

/**
 * Traduce entre el modelo de dominio y el documento de Mongo.
 *
 * <p>Se hace a mano y sin librerias de mapeo: son tres tipos pequenos y asi el
 * dominio no necesita getters/setters ni constructores vacios impuestos por el
 * framework de persistencia.</p>
 */
public final class FranchiseDocumentMapper {

    private FranchiseDocumentMapper() {
    }

    public static FranchiseDocument toDocument(Franchise franchise) {
        return new FranchiseDocument(
                franchise.id(),
                franchise.name(),
                franchise.branches().stream().map(FranchiseDocumentMapper::toDocument).toList());
    }

    public static Franchise toDomain(FranchiseDocument document) {
        return new Franchise(
                document.getId(),
                document.getName(),
                nullSafe(document.getBranches()).stream().map(FranchiseDocumentMapper::toDomain).toList());
    }

    private static BranchDocument toDocument(Branch branch) {
        return new BranchDocument(
                branch.id(),
                branch.name(),
                branch.products().stream().map(FranchiseDocumentMapper::toDocument).toList());
    }

    private static Branch toDomain(BranchDocument document) {
        return new Branch(
                document.getId(),
                document.getName(),
                nullSafe(document.getProducts()).stream().map(FranchiseDocumentMapper::toDomain).toList());
    }

    private static ProductDocument toDocument(Product product) {
        return new ProductDocument(product.id(), product.name(), product.stock());
    }

    private static Product toDomain(ProductDocument document) {
        return new Product(document.getId(), document.getName(), document.getStock());
    }

    /** Un documento guardado sin sucursales/productos llega con la lista nula. */
    private static <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
