package com.accenture.franchise.domain.exception;

/**
 * Se pidio una entidad que no existe. La capa web la mapea a HTTP 404.
 */
public class NotFoundException extends DomainException {

    private NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException franchise(String franchiseId) {
        return new NotFoundException("No existe la franquicia con id '" + franchiseId + "'");
    }

    public static NotFoundException branch(String branchId) {
        return new NotFoundException("No existe la sucursal con id '" + branchId + "'");
    }

    public static NotFoundException product(String productId) {
        return new NotFoundException("No existe el producto con id '" + productId + "'");
    }
}
