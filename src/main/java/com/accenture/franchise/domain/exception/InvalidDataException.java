package com.accenture.franchise.domain.exception;

/**
 * Se violo una invariante del modelo (nombre vacio, stock negativo...).
 * La capa web la mapea a HTTP 400.
 *
 * <p>El dominio valida aunque los DTO ya tengan Bean Validation: el modelo
 * debe ser correcto por si mismo, sin depender de quien lo invoque.</p>
 */
public class InvalidDataException extends DomainException {

    public InvalidDataException(String message) {
        super(message);
    }
}
