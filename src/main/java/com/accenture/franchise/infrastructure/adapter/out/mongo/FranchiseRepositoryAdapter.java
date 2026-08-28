package com.accenture.franchise.infrastructure.adapter.out.mongo;

import com.accenture.franchise.domain.model.Franchise;
import com.accenture.franchise.domain.port.out.FranchiseRepositoryPort;
import com.accenture.franchise.infrastructure.adapter.out.mongo.mapper.FranchiseDocumentMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adaptador de salida: implementa el puerto del dominio con Mongo reactivo.
 *
 * <p>Su unica responsabilidad es traducir entre el modelo de dominio y el
 * documento; ninguna regla de negocio vive aqui.</p>
 */
@Repository
public class FranchiseRepositoryAdapter implements FranchiseRepositoryPort {

    private final FranchiseMongoRepository repository;

    public FranchiseRepositoryAdapter(FranchiseMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        return repository.save(FranchiseDocumentMapper.toDocument(franchise))
                .map(FranchiseDocumentMapper::toDomain);
    }

    @Override
    public Mono<Franchise> findById(String franchiseId) {
        return repository.findById(franchiseId)
                .map(FranchiseDocumentMapper::toDomain);
    }

    @Override
    public Flux<Franchise> findAll() {
        return repository.findAll()
                .map(FranchiseDocumentMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public Mono<Boolean> existsByNameExcludingId(String name, String franchiseId) {
        return repository.existsByNameIgnoreCaseAndIdNot(name, franchiseId);
    }
}
