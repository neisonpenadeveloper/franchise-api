package com.accenture.franchise.domain.model;

import com.accenture.franchise.domain.exception.InvalidDataException;

import java.util.UUID;

/**
 * Producto ofertado en una sucursal: un nombre y una cantidad de stock.
 *
 * <p>Es un value object inmutable: cada operacion devuelve una instancia nueva
 * en lugar de mutar la actual. Eso permite razonar sobre el estado sin efectos
 * colaterales, que es lo que se espera en un flujo reactivo donde el objeto
 * puede viajar entre hilos.</p>
 *
 * @param id    identificador estable del producto dentro de la sucursal
 * @param name  nombre del producto
 * @param stock unidades disponibles; nunca negativo
 */
public record Product(String id, String name, int stock) {

    public Product {
        id = Names.requireId(id, "product.id");
        name = Names.require(name, "product.name");
        if (stock < 0) {
            throw new InvalidDataException("El stock del producto no puede ser negativo");
        }
    }

    /** Crea un producto nuevo con identificador generado. */
    public static Product create(String name, int stock) {
        return new Product(UUID.randomUUID().toString(), name, stock);
    }

    public Product renameTo(String newName) {
        return new Product(id, newName, stock);
    }

    public Product withStock(int newStock) {
        return new Product(id, name, newStock);
    }

    public boolean hasName(String other) {
        return Names.sameName(name, other);
    }
}
