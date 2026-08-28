package com.accenture.franchise.application.usecase;

import com.accenture.franchise.domain.exception.DuplicateNameException;
import com.accenture.franchise.domain.exception.NotFoundException;
import com.accenture.franchise.domain.model.Branch;
import com.accenture.franchise.domain.model.BranchTopProduct;
import com.accenture.franchise.domain.model.Franchise;
import com.accenture.franchise.domain.model.Product;
import com.accenture.franchise.domain.port.out.FranchiseRepositoryPort;
import java.util.function.UnaryOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Casos de uso de la franquicia. Orquesta el puerto de persistencia y el modelo:
 * lee el agregado, delega la regla de negocio en el dominio y guarda el resultado.
 *
 * <p>No conoce Spring ni Mongo ni HTTP; se instancia desde
 * {@code infrastructure.config.BeanConfiguration}. Todas las operaciones siguen
 * el mismo patron funcional: {@code findById -> map(regla) -> save}, sin
 * bloquear en ningun punto.</p>
 */
public class FranchiseUseCase {

    private final FranchiseRepositoryPort repository;

    public FranchiseUseCase(FranchiseRepositoryPort repository) {
        this.repository = repository;
    }

    // ---------- Franquicia ----------

    /** Registra una franquicia nueva. Falla si el nombre ya esta en uso. */
    public Mono<Franchise> createFranchise(String name) {
        return Mono.fromCallable(() -> Franchise.create(name))
                .flatMap(franchise -> repository.existsByName(franchise.name())
                        .filter(exists -> !exists)
                        .switchIfEmpty(Mono.error(() -> DuplicateNameException.franchise(franchise.name())))
                        .thenReturn(franchise))
                .flatMap(repository::save);
    }

    public Flux<Franchise> findAll() {
        return repository.findAll();
    }

    public Mono<Franchise> findById(String franchiseId) {
        return loadFranchise(franchiseId);
    }

    /**
     * Punto extra: renombrar la franquicia.
     *
     * <p>Se renombra primero (asi el dominio valida el nombre) y luego se
     * comprueba el conflicto contra las demas franquicias, excluyendo la
     * propia para que renombrarla a su mismo nombre no sea un conflicto.</p>
     */
    public Mono<Franchise> renameFranchise(String franchiseId, String newName) {
        return loadFranchise(franchiseId)
                .map(franchise -> franchise.renameTo(newName))
                .flatMap(renamed -> repository.existsByNameExcludingId(renamed.name(), renamed.id())
                        .filter(exists -> !exists)
                        .switchIfEmpty(Mono.error(() -> DuplicateNameException.franchise(newName)))
                        .thenReturn(renamed))
                .flatMap(repository::save);
    }

    // ---------- Sucursales ----------

    /** Agrega una sucursal a una franquicia existente. */
    public Mono<Franchise> addBranch(String franchiseId, String branchName) {
        return mutate(franchiseId, franchise -> franchise.addBranch(Branch.create(branchName)));
    }

    /** Punto extra: renombrar una sucursal. */
    public Mono<Franchise> renameBranch(String franchiseId, String branchId, String newName) {
        return mutate(franchiseId, franchise -> franchise.renameBranch(branchId, newName));
    }

    // ---------- Productos ----------

    /** Agrega un producto a una sucursal. */
    public Mono<Franchise> addProduct(String franchiseId, String branchId, String productName, int stock) {
        return mutate(franchiseId, franchise -> franchise.addProduct(branchId, Product.create(productName, stock)));
    }

    /** Elimina un producto de una sucursal. */
    public Mono<Franchise> removeProduct(String franchiseId, String branchId, String productId) {
        return mutate(franchiseId, franchise -> franchise.removeProduct(branchId, productId));
    }

    /** Modifica el stock de un producto. */
    public Mono<Franchise> updateProductStock(String franchiseId, String branchId, String productId, int stock) {
        return mutate(franchiseId, franchise -> franchise.updateProductStock(branchId, productId, stock));
    }

    /** Punto extra: renombrar un producto. */
    public Mono<Franchise> renameProduct(String franchiseId, String branchId, String productId, String newName) {
        return mutate(franchiseId, franchise -> franchise.renameProduct(branchId, productId, newName));
    }

    // ---------- Consultas ----------

    /**
     * Producto con mas stock de cada sucursal de la franquicia, indicando a que
     * sucursal pertenece. El calculo vive en el dominio; aqui solo se aplana.
     */
    public Flux<BranchTopProduct> topStockProductPerBranch(String franchiseId) {
        return loadFranchise(franchiseId)
                .flatMapIterable(Franchise::topStockProductPerBranch);
    }

    // ---------- Apoyo ----------

    private Mono<Franchise> loadFranchise(String franchiseId) {
        return repository.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)));
    }

    /**
     * Lee el agregado, le aplica la regla de negocio y lo guarda.
     *
     * <p>La regla se ejecuta dentro de {@code map}, de modo que las excepciones
     * de dominio viajan por el canal de error del {@code Mono} y las traduce el
     * manejador global.</p>
     */
    private Mono<Franchise> mutate(String franchiseId, UnaryOperator<Franchise> operation) {
        return loadFranchise(franchiseId)
                .map(operation)
                .flatMap(repository::save);
    }
}
