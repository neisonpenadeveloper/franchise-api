package com.accenture.franchise.domain.model;

import com.accenture.franchise.domain.exception.DuplicateNameException;
import com.accenture.franchise.domain.exception.NotFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Sucursal de una franquicia: un nombre y el listado de productos que oferta.
 *
 * <p>Inmutable, igual que {@link Product}. Es la frontera de consistencia de los
 * productos: aqui se garantiza que no haya dos productos con el mismo nombre.</p>
 *
 * @param id       identificador estable de la sucursal dentro de la franquicia
 * @param name     nombre de la sucursal
 * @param products productos ofertados; nunca nulo, siempre copia defensiva
 */
public record Branch(String id, String name, List<Product> products) {

    public Branch {
        id = Names.requireId(id, "branch.id");
        name = Names.require(name, "branch.name");
        products = products == null ? List.of() : List.copyOf(products);
    }

    /** Crea una sucursal nueva, sin productos, con identificador generado. */
    public static Branch create(String name) {
        return new Branch(UUID.randomUUID().toString(), name, List.of());
    }

    public Branch renameTo(String newName) {
        return new Branch(id, newName, products);
    }

    public boolean hasName(String other) {
        return Names.sameName(name, other);
    }

    public Optional<Product> findProduct(String productId) {
        return products.stream().filter(p -> p.id().equals(productId)).findFirst();
    }

    /**
     * Agrega un producto.
     *
     * @throws DuplicateNameException si ya existe otro producto con ese nombre
     */
    public Branch addProduct(Product product) {
        if (products.stream().anyMatch(p -> p.hasName(product.name()))) {
            throw DuplicateNameException.product(product.name());
        }
        List<Product> updated = new ArrayList<>(products);
        updated.add(product);
        return new Branch(id, name, updated);
    }

    /**
     * Elimina un producto.
     *
     * @throws NotFoundException si el producto no pertenece a la sucursal
     */
    public Branch removeProduct(String productId) {
        List<Product> updated = products.stream()
                .filter(p -> !p.id().equals(productId))
                .toList();
        if (updated.size() == products.size()) {
            throw NotFoundException.product(productId);
        }
        return new Branch(id, name, updated);
    }

    public Branch updateProductStock(String productId, int newStock) {
        return replaceProduct(productId, product -> product.withStock(newStock));
    }

    /**
     * Cambia el nombre de un producto validando que no colisione con otro.
     */
    public Branch renameProduct(String productId, String newName) {
        boolean taken = products.stream()
                .anyMatch(p -> !p.id().equals(productId) && p.hasName(newName));
        if (taken) {
            throw DuplicateNameException.product(newName);
        }
        return replaceProduct(productId, product -> product.renameTo(newName));
    }

    /** Producto con mayor stock de la sucursal; vacio si la sucursal no tiene productos. */
    public Optional<Product> topStockProduct() {
        return products.stream().max(Comparator.comparingInt(Product::stock));
    }

    private Branch replaceProduct(String productId, UnaryOperator<Product> operation) {
        Product current = findProduct(productId).orElseThrow(() -> NotFoundException.product(productId));
        List<Product> updated = products.stream()
                .map(p -> p.id().equals(productId) ? operation.apply(current) : p)
                .toList();
        return new Branch(id, name, updated);
    }
}
