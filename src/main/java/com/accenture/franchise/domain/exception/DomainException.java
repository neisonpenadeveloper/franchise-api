package com.accenture.franchise.domain.exception;

/**
 * Raiz de todos los errores de negocio.
 *
 * <p>Es una excepcion no chequeada para no contaminar las firmas reactivas
 * ({@code Mono.error(...)} la propaga por el canal de error del flujo).
 * La capa web la traduce a un codigo HTTP en el manejador global.</p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
