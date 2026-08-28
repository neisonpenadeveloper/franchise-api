package com.accenture.franchise.domain.model;

import com.accenture.franchise.domain.exception.InvalidDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("create genera un identificador y normaliza el nombre")
    void createGeneratesIdAndTrimsName() {
        Product product = Product.create("  Cafe 500g  ", 10);

        assertThat(product.id()).isNotBlank();
        assertThat(product.name()).isEqualTo("Cafe 500g");
        assertThat(product.stock()).isEqualTo(10);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("el nombre del producto es obligatorio")
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> Product.create(name, 1))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("product.name");
    }

    @Test
    @DisplayName("el stock no puede ser negativo")
    void rejectsNegativeStock() {
        assertThatThrownBy(() -> Product.create("Cafe", -1))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("stock");
    }

    @Test
    @DisplayName("withStock y renameTo devuelven copias y conservan el id")
    void copiesKeepIdentity() {
        Product original = Product.create("Cafe", 10);

        Product withStock = original.withStock(25);
        Product renamed = original.renameTo("Cafe premium");

        assertThat(withStock.id()).isEqualTo(original.id());
        assertThat(withStock.stock()).isEqualTo(25);
        assertThat(renamed.name()).isEqualTo("Cafe premium");
        assertThat(original.stock()).isEqualTo(10);
        assertThat(original.name()).isEqualTo("Cafe");
    }

    @Test
    @DisplayName("hasName compara sin distinguir mayusculas ni espacios")
    void nameComparisonIsLenient() {
        Product product = Product.create("Cafe", 1);

        assertThat(product.hasName("  cafe ")).isTrue();
        assertThat(product.hasName("te")).isFalse();
    }
}
