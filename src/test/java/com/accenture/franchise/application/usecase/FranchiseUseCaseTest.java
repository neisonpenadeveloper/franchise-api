package com.accenture.franchise.application.usecase;

import com.accenture.franchise.domain.exception.DuplicateNameException;
import com.accenture.franchise.domain.exception.NotFoundException;
import com.accenture.franchise.domain.model.Branch;
import com.accenture.franchise.domain.model.BranchTopProduct;
import com.accenture.franchise.domain.model.Franchise;
import com.accenture.franchise.domain.model.Product;
import com.accenture.franchise.domain.port.out.FranchiseRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba los casos de uso con el puerto de persistencia simulado: se verifica
 * la orquestacion (que consulte, que guarde, que propague el error), no la
 * regla de negocio, que ya se cubre en los tests de dominio.
 */
@ExtendWith(MockitoExtension.class)
class FranchiseUseCaseTest {

    private static final String FRANCHISE_ID = "f1";

    @Mock
    private FranchiseRepositoryPort repository;

    private FranchiseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FranchiseUseCase(repository);
    }

    /** Devuelve la franquicia recibida, como haria Mongo al guardar. */
    private void stubSave() {
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("createFranchise guarda cuando el nombre esta libre")
    void createFranchise() {
        when(repository.existsByName(anyString())).thenReturn(Mono.just(false));
        stubSave();

        StepVerifier.create(useCase.createFranchise("Centro"))
                .assertNext(franchise -> {
                    assertThat(franchise.id()).isNotBlank();
                    assertThat(franchise.name()).isEqualTo("Centro");
                    assertThat(franchise.branches()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createFranchise falla y no guarda si el nombre ya existe")
    void createFranchiseWithTakenName() {
        when(repository.existsByName(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.createFranchise("Centro"))
                .expectError(DuplicateNameException.class)
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("las operaciones sobre una franquicia inexistente devuelven NotFound")
    void unknownFranchise() {
        when(repository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.addBranch(FRANCHISE_ID, "Norte"))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("addBranch agrega la sucursal y persiste la franquicia completa")
    void addBranch() {
        when(repository.findById(FRANCHISE_ID))
                .thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Centro", List.of())));
        stubSave();

        StepVerifier.create(useCase.addBranch(FRANCHISE_ID, "Norte"))
                .assertNext(franchise -> assertThat(franchise.branches())
                        .extracting(Branch::name).containsExactly("Norte"))
                .verifyComplete();

        ArgumentCaptor<Franchise> saved = ArgumentCaptor.forClass(Franchise.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().branches()).hasSize(1);
    }

    @Test
    @DisplayName("addProduct agrega el producto a la sucursal indicada")
    void addProduct() {
        when(repository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchiseWithBranch()));
        stubSave();

        StepVerifier.create(useCase.addProduct(FRANCHISE_ID, "b1", "Cafe", 10))
                .assertNext(franchise -> assertThat(franchise.findBranch("b1")).get()
                        .extracting(branch -> branch.products().getFirst().name()).isEqualTo("Cafe"))
                .verifyComplete();
    }

    @Test
    @DisplayName("removeProduct elimina el producto de la sucursal")
    void removeProduct() {
        Product cafe = Product.create("Cafe", 10);
        when(repository.findById(FRANCHISE_ID)).thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Centro",
                List.of(new Branch("b1", "Norte", List.of(cafe))))));
        stubSave();

        StepVerifier.create(useCase.removeProduct(FRANCHISE_ID, "b1", cafe.id()))
                .assertNext(franchise -> assertThat(franchise.findBranch("b1")).get()
                        .extracting(branch -> branch.products().size()).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateProductStock modifica el stock del producto")
    void updateProductStock() {
        Product cafe = Product.create("Cafe", 10);
        when(repository.findById(FRANCHISE_ID)).thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Centro",
                List.of(new Branch("b1", "Norte", List.of(cafe))))));
        stubSave();

        StepVerifier.create(useCase.updateProductStock(FRANCHISE_ID, "b1", cafe.id(), 77))
                .assertNext(franchise -> assertThat(franchise.findBranch("b1")).get()
                        .extracting(branch -> branch.products().getFirst().stock()).isEqualTo(77))
                .verifyComplete();
    }

    @Test
    @DisplayName("renameFranchise admite conservar el mismo nombre")
    void renameFranchiseToSameName() {
        when(repository.findById(FRANCHISE_ID))
                .thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Centro", List.of())));
        when(repository.existsByNameExcludingId("Centro", FRANCHISE_ID)).thenReturn(Mono.just(false));
        stubSave();

        StepVerifier.create(useCase.renameFranchise(FRANCHISE_ID, "Centro"))
                .assertNext(franchise -> assertThat(franchise.name()).isEqualTo("Centro"))
                .verifyComplete();
    }

    @Test
    @DisplayName("renameFranchise falla si otra franquicia ya usa el nombre")
    void renameFranchiseToTakenName() {
        when(repository.findById(FRANCHISE_ID))
                .thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Centro", List.of())));
        when(repository.existsByNameExcludingId("Sur", FRANCHISE_ID)).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.renameFranchise(FRANCHISE_ID, "Sur"))
                .expectError(DuplicateNameException.class)
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("topStockProductPerBranch emite un producto por sucursal")
    void topStockProductPerBranch() {
        when(repository.findById(FRANCHISE_ID)).thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Centro",
                List.of(
                        new Branch("b1", "Norte", List.of(Product.create("Cafe", 10), Product.create("Te", 40))),
                        new Branch("b2", "Sur", List.of(Product.create("Pan", 7)))))));

        StepVerifier.create(useCase.topStockProductPerBranch(FRANCHISE_ID))
                .assertNext(top -> assertNameAndBranch(top, "Te", "Norte"))
                .assertNext(top -> assertNameAndBranch(top, "Pan", "Sur"))
                .verifyComplete();
    }

    private static void assertNameAndBranch(BranchTopProduct top, String productName, String branchName) {
        assertThat(top.product().name()).isEqualTo(productName);
        assertThat(top.branchName()).isEqualTo(branchName);
    }

    private static Franchise franchiseWithBranch() {
        return new Franchise(FRANCHISE_ID, "Centro", List.of(new Branch("b1", "Norte", List.of())));
    }
}
