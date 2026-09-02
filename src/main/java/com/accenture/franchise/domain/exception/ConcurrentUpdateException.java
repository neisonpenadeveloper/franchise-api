package com.accenture.franchise.domain.exception;

/**
 * Dos escrituras simultaneas intentaron modificar la misma franquicia y una
 * llego con una version desactualizada del agregado.
 *
 * <p>La franquicia se modifica leyendo el agregado completo, aplicando la regla
 * y volviendo a guardarlo. Si entre la lectura y la escritura otra peticion ya
 * guardo el documento, escribir sin mas borraria ese cambio (lectura sucia y
 * actualizacion perdida). El control de versiones del documento detecta ese
 * choque y lo convierte en este error.</p>
 *
 * <p>Vive en el dominio, y no en la infraestructura, para que el caso de uso
 * pueda reaccionar sin conocer el motor de persistencia. Es el adaptador quien
 * traduce la excepcion tecnica de Mongo a esta.</p>
 */
public class ConcurrentUpdateException extends DomainException {

    public ConcurrentUpdateException(String franchiseId) {
        super("La franquicia con id '" + franchiseId
                + "' fue modificada por otra operacion simultanea; vuelva a intentarlo");
    }
}
