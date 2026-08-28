package com.accenture.franchise.infrastructure.adapter.out.mongo;

import com.accenture.franchise.infrastructure.adapter.out.mongo.document.FranchiseDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio de Spring Data reactivo sobre la coleccion {@code franchises}.
 * Es un detalle de infraestructura: el resto de la aplicacion habla con
 * {@code FranchiseRepositoryPort}, no con esta interfaz.
 */
public interface FranchiseMongoRepository extends ReactiveMongoRepository<FranchiseDocument, String> {

    Mono<Boolean> existsByNameIgnoreCase(String name);

    Mono<Boolean> existsByNameIgnoreCaseAndIdNot(String name, String id);
}
