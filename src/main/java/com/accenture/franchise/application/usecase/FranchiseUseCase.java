package com.accenture.franchise.application.usecase;

import com.accenture.franchise.domain.exception.ConcurrentUpdateException;
import com.accenture.franchise.domain.exception.DuplicateNameException;
import com.accenture.franchise.domain.exception.NotFoundException;
import com.accenture.franchise.domain.model.Branch;
import com.accenture.franchise.domain.model.BranchTopProduct;
import com.accenture.franchise.domain.model.Franchise;
import com.accenture.franchise.domain.model.Product;
import com.accenture.franchise.domain.port.out.FranchiseRepositoryPort;
import java.time.Duration;
import java.util.function.UnaryOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

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

    /**
     * Reintentos ante un choque de versiones.
     *
     * <p>Leer y volver a guardar el agregado deja una ventana en la que otra
     * peticion puede escribir primero. Cuando eso pasa, el guardado falla con
     * {@link ConcurrentUpdateException} en lugar de pisar el cambio ajeno, y
     * aqui se reintenta la operacion entera: al repetirse se vuelve a leer la
     * franquicia, ya con el cambio de la otra peticion incluido, y la regla se
     * aplica sobre el estado actual.</p>
     *
     * <p>La espera entre intentos crece (20 ms, 40 ms, 80 ms) con una variacion
     * aleatoria, para que dos peticiones que chocan no vuelvan a intentarlo en
     * el mismo instante. Es una espera reactiva: no bloquea ningun hilo. Si tras
     * los tres intentos sigue habiendo conflicto se propaga el error original y
     * la capa web responde 409 en vez de mentirle al cliente.</p>
     */
    private static final Retry ON_CONFLICT = Retry
            .backoff(3, Duration.ofMillis(20))
            .filter(ConcurrentUpdateException.class::isInstance)
            .onRetryExhaustedThrow((specification, signal) -> signal.failure());

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
                .flatMap(repository::save)
                .retryWhen(ON_CONFLICT);
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
     *
     * <p>Toda la secuencia va dentro del reintento y no solo el guardado: si
     * solo se repitiera {@code save} se volveria a enviar el mismo estado
     * caduco y el conflicto se repetiria para siempre.</p>
     */
    private Mono<Franchise> mutate(String franchiseId, UnaryOperator<Franchise> operation) {
        return loadFranchise(franchiseId)
                .map(operation)
                .flatMap(repository::save)
                .retryWhen(ON_CONFLICT);
    }
}
