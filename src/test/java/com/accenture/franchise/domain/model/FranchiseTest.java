package com.accenture.franchise.domain.model;

import com.accenture.franchise.domain.exception.DuplicateNameException;
import com.accenture.franchise.domain.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FranchiseTest {

    @Test
    @DisplayName("addBranch rechaza sucursales con nombre repetido")
    void rejectsDuplicateBranchName() {
        Franchise franchise = Franchise.create("Franquicia Centro").addBranch(Branch.create("Norte"));

        assertThatThrownBy(() -> franchise.addBranch(Branch.create("norte")))
                .isInstanceOf(DuplicateNameException.class);
    }

    @Test
    @DisplayName("las operaciones sobre una sucursal inexistente fallan con NotFound")
    void unknownBranchFails() {
        Franchise franchise = Franchise.create("Centro");

        assertThatThrownBy(() -> franchise.addProduct("inexistente", Product.create("Cafe", 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("addProduct delega en la sucursal y devuelve la franquicia actualizada")
    void addProductToBranch() {
        Branch branch = Branch.create("Norte");
        Franchise franchise = Franchise.create("Centro").addBranch(branch);

        Franchise updated = franchise.addProduct(branch.id(), Product.create("Cafe", 10));

        assertThat(updated.findBranch(branch.id())).get()
                .extracting(b -> b.products().size()).isEqualTo(1);
        assertThat(franchise.findBranch(branch.id())).get()
                .extracting(b -> b.products().size()).isEqualTo(0);
    }

    @Test
    @DisplayName("renameBranch rechaza el nombre de otra sucursal")
    void renameBranchToTakenName() {
        Branch norte = Branch.create("Norte");
        Franchise franchise = Franchise.create("Centro").addBranch(norte).addBranch(Branch.create("Sur"));

        assertThatThrownBy(() -> franchise.renameBranch(norte.id(), "Sur"))
                .isInstanceOf(DuplicateNameException.class);
    }

    @Test
    @DisplayName("topStockProductPerBranch devuelve un producto por sucursal con su sucursal")
    void topStockProductPerBranch() {
        Franchise franchise = new Franchise("f1", "Centro", List.of(
                new Branch("b1", "Norte", List.of(Product.create("Cafe", 10), Product.create("Te", 40))),
                new Branch("b2", "Sur", List.of(Product.create("Pan", 7), Product.create("Leche", 3)))));

        List<BranchTopProduct> top = franchise.topStockProductPerBranch();

        assertThat(top).hasSize(2);
        assertThat(top).extracting(BranchTopProduct::branchName).containsExactly("Norte", "Sur");
        assertThat(top).extracting(item -> item.product().name()).containsExactly("Te", "Pan");
    }

    @Test
    @DisplayName("las sucursales sin productos no aparecen en el top de stock")
    void topStockSkipsEmptyBranches() {
        Franchise franchise = new Franchise("f1", "Centro", List.of(
                new Branch("b1", "Norte", List.of(Product.create("Cafe", 10))),
                Branch.create("Sur")));

        assertThat(franchise.topStockProductPerBranch())
                .extracting(BranchTopProduct::branchName)
                .containsExactly("Norte");
    }

    @Test
    @DisplayName("la lista de sucursales es inmutable hacia afuera")
    void branchesAreImmutable() {
        Franchise franchise = Franchise.create("Centro").addBranch(Branch.create("Norte"));

        assertThatThrownBy(() -> franchise.branches().add(Branch.create("Sur")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
