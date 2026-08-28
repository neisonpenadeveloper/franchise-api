package com.accenture.franchise.domain.model;

import com.accenture.franchise.domain.exception.DuplicateNameException;
import com.accenture.franchise.domain.exception.InvalidDataException;
import com.accenture.franchise.domain.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchTest {

    @Test
    @DisplayName("una sucursal nueva no tiene productos")
    void createStartsEmpty() {
        Branch branch = Branch.create("Sucursal Norte");

        assertThat(branch.id()).isNotBlank();
        assertThat(branch.products()).isEmpty();
    }

    @Test
    @DisplayName("addProduct agrega sin mutar la sucursal original")
    void addProductIsImmutable() {
        Branch branch = Branch.create("Norte");

        Branch updated = branch.addProduct(Product.create("Cafe", 10));

        assertThat(branch.products()).isEmpty();
        assertThat(updated.products()).hasSize(1);
    }

    @Test
    @DisplayName("no se admiten dos productos con el mismo nombre")
    void rejectsDuplicateProductName() {
        Branch branch = Branch.create("Norte").addProduct(Product.create("Cafe", 10));

        assertThatThrownBy(() -> branch.addProduct(Product.create("  cafe ", 5)))
                .isInstanceOf(DuplicateNameException.class);
    }

    @Test
    @DisplayName("removeProduct falla si el producto no pertenece a la sucursal")
    void removeUnknownProductFails() {
        Branch branch = Branch.create("Norte");

        assertThatThrownBy(() -> branch.removeProduct("inexistente"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("removeProduct deja fuera solo al producto indicado")
    void removeProduct() {
        Product cafe = Product.create("Cafe", 10);
        Product te = Product.create("Te", 4);
        Branch branch = Branch.create("Norte").addProduct(cafe).addProduct(te);

        Branch updated = branch.removeProduct(cafe.id());

        assertThat(updated.products()).extracting(Product::name).containsExactly("Te");
    }

    @Test
    @DisplayName("updateProductStock cambia solo el producto indicado")
    void updateStock() {
        Product cafe = Product.create("Cafe", 10);
        Branch branch = Branch.create("Norte").addProduct(cafe).addProduct(Product.create("Te", 4));

        Branch updated = branch.updateProductStock(cafe.id(), 99);

        assertThat(updated.findProduct(cafe.id())).get().extracting(Product::stock).isEqualTo(99);
        assertThat(updated.products()).hasSize(2);
    }

    @Test
    @DisplayName("updateProductStock rechaza un stock negativo")
    void updateStockRejectsNegative() {
        Product cafe = Product.create("Cafe", 10);
        Branch branch = Branch.create("Norte").addProduct(cafe);

        assertThatThrownBy(() -> branch.updateProductStock(cafe.id(), -5))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    @DisplayName("renameProduct rechaza el nombre de otro producto pero admite el propio")
    void renameProduct() {
        Product cafe = Product.create("Cafe", 10);
        Product te = Product.create("Te", 4);
        Branch branch = Branch.create("Norte").addProduct(cafe).addProduct(te);

        assertThatThrownBy(() -> branch.renameProduct(cafe.id(), "Te"))
                .isInstanceOf(DuplicateNameException.class);
        assertThat(branch.renameProduct(cafe.id(), "Cafe").findProduct(cafe.id()))
                .get().extracting(Product::name).isEqualTo("Cafe");
    }

    @Test
    @DisplayName("topStockProduct devuelve el producto de mayor stock")
    void topStockProduct() {
        Branch branch = new Branch("b1", "Norte", List.of(
                Product.create("Cafe", 10),
                Product.create("Te", 40),
                Product.create("Pan", 25)));

        assertThat(branch.topStockProduct()).get().extracting(Product::name).isEqualTo("Te");
    }

    @Test
    @DisplayName("topStockProduct esta vacio si la sucursal no tiene productos")
    void topStockProductWithoutProducts() {
        assertThat(Branch.create("Norte").topStockProduct()).isEmpty();
    }
}
