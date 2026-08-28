package com.accenture.franchise.infrastructure.adapter.in.web;

import com.accenture.franchise.application.usecase.FranchiseUseCase;
import com.accenture.franchise.domain.exception.DuplicateNameException;
import com.accenture.franchise.domain.exception.NotFoundException;
import com.accenture.franchise.domain.model.Branch;
import com.accenture.franchise.domain.model.BranchTopProduct;
import com.accenture.franchise.domain.model.Franchise;
import com.accenture.franchise.domain.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Prueba la capa web aislada: el caso de uso esta simulado, de modo que aqui
 * solo se verifica el contrato HTTP (rutas, codigos, validacion y traduccion
 * de errores de dominio).
 */
@WebFluxTest(controllers = FranchiseController.class)
class FranchiseControllerTest {

    private static final String BASE_URL = "/api/v1/franchises";

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private FranchiseUseCase useCase;

    private static Franchise sampleFranchise() {
        return new Franchise("f1", "Centro",
                List.of(new Branch("b1", "Norte", List.of(new Product("p1", "Cafe", 10)))));
    }

    @Test
    @DisplayName("POST /franchises responde 201 con la franquicia creada")
    void createFranchise() {
        when(useCase.createFranchise(anyString())).thenReturn(Mono.just(sampleFranchise()));

        webClient.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Centro"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("f1")
                .jsonPath("$.name").isEqualTo("Centro")
                .jsonPath("$.branches[0].products[0].name").isEqualTo("Cafe");
    }

    @Test
    @DisplayName("POST /franchises con nombre vacio responde 400 con el detalle del campo")
    void createFranchiseWithBlankName() {
        webClient.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", " "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("name"));
    }

    @Test
    @DisplayName("un nombre duplicado se traduce a 409")
    void duplicateNameIsConflict() {
        when(useCase.createFranchise(anyString()))
                .thenReturn(Mono.error(DuplicateNameException.franchise("Centro")));

        webClient.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Centro"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.error").isEqualTo("DUPLICATE_NAME");
    }

    @Test
    @DisplayName("una franquicia inexistente se traduce a 404")
    void notFoundIsTranslated() {
        when(useCase.findById("desconocida")).thenReturn(Mono.error(NotFoundException.franchise("desconocida")));

        webClient.get().uri(BASE_URL + "/desconocida")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.error").isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("POST /franchises/{id}/branches responde 201")
    void addBranch() {
        when(useCase.addBranch(anyString(), anyString())).thenReturn(Mono.just(sampleFranchise()));

        webClient.post().uri(BASE_URL + "/f1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Norte"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.branches[0].name").isEqualTo("Norte");
    }

    @Test
    @DisplayName("POST de producto responde 201")
    void addProduct() {
        when(useCase.addProduct(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Mono.just(sampleFranchise()));

        webClient.post().uri(BASE_URL + "/f1/branches/b1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Cafe", "stock", 10))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.branches[0].products[0].stock").isEqualTo(10);
    }

    @Test
    @DisplayName("un stock negativo se rechaza con 400 antes de llegar al caso de uso")
    void addProductWithNegativeStock() {
        webClient.post().uri(BASE_URL + "/f1/branches/b1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Cafe", "stock", -3))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("DELETE de producto responde 200 con la franquicia resultante")
    void removeProduct() {
        when(useCase.removeProduct("f1", "b1", "p1")).thenReturn(Mono.just(sampleFranchise()));

        webClient.delete().uri(BASE_URL + "/f1/branches/b1/products/p1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo("f1");
    }

    @Test
    @DisplayName("PATCH de stock responde 200")
    void updateStock() {
        when(useCase.updateProductStock("f1", "b1", "p1", 50)).thenReturn(Mono.just(sampleFranchise()));

        webClient.patch().uri(BASE_URL + "/f1/branches/b1/products/p1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("stock", 50))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET del top de stock devuelve el producto y su sucursal")
    void topStockProducts() {
        when(useCase.topStockProductPerBranch("f1")).thenReturn(Flux.just(
                new BranchTopProduct("b1", "Norte", new Product("p1", "Cafe", 10)),
                new BranchTopProduct("b2", "Sur", new Product("p2", "Pan", 7))));

        webClient.get().uri(BASE_URL + "/f1/top-stock-products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].branchName").isEqualTo("Norte")
                .jsonPath("$[0].product.name").isEqualTo("Cafe")
                .jsonPath("$[1].branchName").isEqualTo("Sur")
                .jsonPath("$[1].product.stock").isEqualTo(7);
    }

    @Test
    @DisplayName("PATCH del nombre de la sucursal responde 200")
    void renameBranch() {
        when(useCase.renameBranch("f1", "b1", "Norte")).thenReturn(Mono.just(sampleFranchise()));

        webClient.patch().uri(BASE_URL + "/f1/branches/b1/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Norte"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("PATCH del nombre del producto responde 200")
    void renameProduct() {
        when(useCase.renameProduct("f1", "b1", "p1", "Cafe")).thenReturn(Mono.just(sampleFranchise()));

        webClient.patch().uri(BASE_URL + "/f1/branches/b1/products/p1/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Cafe"))
                .exchange()
                .expectStatus().isOk();
    }
}
