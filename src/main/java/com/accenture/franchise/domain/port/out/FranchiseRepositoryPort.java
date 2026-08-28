package com.accenture.franchise.domain.port.out;

import com.accenture.franchise.domain.model.Franchise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida hacia la persistencia. El dominio define el contrato y la
 * infraestructura lo implementa (adaptador de Mongo), de modo que la regla de
 * dependencia apunta siempre hacia adentro.
 *
 * <p>El puerto habla en tipos de Reactor a proposito: la aplicacion es reactiva
 * de extremo a extremo y devolver {@code Franchise} plano obligaria a bloquear
 * en algun punto. Reactor es una libreria, no un framework, y no ata el dominio
 * a Spring ni a Mongo.</p>
 */
public interface FranchiseRepositoryPort {

    /** Inserta o actualiza la franquicia completa (raiz del agregado). */
    Mono<Franchise> save(Franchise franchise);

    /** Franquicia por id; {@code Mono.empty()} si no existe. */
    Mono<Franchise> findById(String franchiseId);

    /** Todas las franquicias registradas. */
    Flux<Franchise> findAll();

    /** {@code true} si ya hay una franquicia con ese nombre (sin distinguir mayusculas). */
    Mono<Boolean> existsByName(String name);

    /**
     * Igual que {@link #existsByName(String)} pero ignorando una franquicia.
     *
     * <p>Necesario al renombrar: la propia franquicia no debe contar como
     * conflicto consigo misma.</p>
     */
    Mono<Boolean> existsByNameExcludingId(String name, String franchiseId);
}
