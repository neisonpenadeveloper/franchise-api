package com.accenture.franchise.domain.exception;

/**
 * Se intento crear un nombre que ya esta en uso dentro del mismo ambito
 * (sucursal dentro de una franquicia, producto dentro de una sucursal).
 * La capa web la mapea a HTTP 409.
 */
public class DuplicateNameException extends DomainException {

    private DuplicateNameException(String message) {
        super(message);
    }

    public static DuplicateNameException branch(String name) {
        return new DuplicateNameException("La franquicia ya tiene una sucursal llamada '" + name + "'");
    }

    public static DuplicateNameException product(String name) {
        return new DuplicateNameException("La sucursal ya tiene un producto llamado '" + name + "'");
    }

    public static DuplicateNameException franchise(String name) {
        return new DuplicateNameException("Ya existe una franquicia llamada '" + name + "'");
    }
}
