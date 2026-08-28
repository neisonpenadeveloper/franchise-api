package com.accenture.franchise.infrastructure.adapter.in.web;

import com.accenture.franchise.application.usecase.FranchiseUseCase;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.BranchTopProductResponse;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.CreateProductRequest;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.ErrorResponse;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.FranchiseResponse;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.NameRequest;
import com.accenture.franchise.infrastructure.adapter.in.web.dto.UpdateStockRequest;
import com.accenture.franchise.infrastructure.adapter.in.web.mapper.FranchiseWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adaptador de entrada HTTP. Traduce peticiones REST a llamadas al caso de uso
 * y el modelo de dominio a DTO de respuesta; no contiene logica de negocio.
 *
 * <p>Las rutas cuelgan de la franquicia porque sucursales y productos no existen
 * fuera de ella. Todas las mutaciones devuelven la franquicia completa para que
 * el cliente vea el estado resultante sin una segunda peticion.</p>
 */
@RestController
@RequestMapping("/api/v1/franchises")
@Tag(name = "Franquicias", description = "Gestion de franquicias, sucursales y productos")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos invalidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nombre duplicado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class FranchiseController {

    private final FranchiseUseCase useCase;

    public FranchiseController(FranchiseUseCase useCase) {
        this.useCase = useCase;
    }

    // ---------- Franquicias ----------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar una nueva franquicia")
    public Mono<FranchiseResponse> createFranchise(@Valid @RequestBody NameRequest request) {
        return useCase.createFranchise(request.name())
                .map(FranchiseWebMapper::toResponse);
    }

    @GetMapping
    @Operation(summary = "Listar todas las franquicias")
    public Flux<FranchiseResponse> findAll() {
        return useCase.findAll()
                .map(FranchiseWebMapper::toResponse);
    }

    @GetMapping("/{franchiseId}")
    @Operation(summary = "Consultar una franquicia por id")
    public Mono<FranchiseResponse> findById(@PathVariable String franchiseId) {
        return useCase.findById(franchiseId)
                .map(FranchiseWebMapper::toResponse);
    }

    @PatchMapping("/{franchiseId}/name")
    @Operation(summary = "Actualizar el nombre de la franquicia")
    public Mono<FranchiseResponse> renameFranchise(@PathVariable String franchiseId,
                                                   @Valid @RequestBody NameRequest request) {
        return useCase.renameFranchise(franchiseId, request.name())
                .map(FranchiseWebMapper::toResponse);
    }

    // ---------- Sucursales ----------

    @PostMapping("/{franchiseId}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar una nueva sucursal a la franquicia")
    public Mono<FranchiseResponse> addBranch(@PathVariable String franchiseId,
                                             @Valid @RequestBody NameRequest request) {
        return useCase.addBranch(franchiseId, request.name())
                .map(FranchiseWebMapper::toResponse);
    }

    @PatchMapping("/{franchiseId}/branches/{branchId}/name")
    @Operation(summary = "Actualizar el nombre de la sucursal")
    public Mono<FranchiseResponse> renameBranch(@PathVariable String franchiseId,
                                                @PathVariable String branchId,
                                                @Valid @RequestBody NameRequest request) {
        return useCase.renameBranch(franchiseId, branchId, request.name())
                .map(FranchiseWebMapper::toResponse);
    }

    // ---------- Productos ----------

    @PostMapping("/{franchiseId}/branches/{branchId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar un nuevo producto a la sucursal")
    public Mono<FranchiseResponse> addProduct(@PathVariable String franchiseId,
                                              @PathVariable String branchId,
                                              @Valid @RequestBody CreateProductRequest request) {
        return useCase.addProduct(franchiseId, branchId, request.name(), request.stock())
                .map(FranchiseWebMapper::toResponse);
    }

    @DeleteMapping("/{franchiseId}/branches/{branchId}/products/{productId}")
    @Operation(summary = "Eliminar un producto de la sucursal")
    public Mono<FranchiseResponse> removeProduct(@PathVariable String franchiseId,
                                                 @PathVariable String branchId,
                                                 @PathVariable String productId) {
        return useCase.removeProduct(franchiseId, branchId, productId)
                .map(FranchiseWebMapper::toResponse);
    }

    @PatchMapping("/{franchiseId}/branches/{branchId}/products/{productId}/stock")
    @Operation(summary = "Modificar el stock de un producto")
    public Mono<FranchiseResponse> updateProductStock(@PathVariable String franchiseId,
                                                      @PathVariable String branchId,
                                                      @PathVariable String productId,
                                                      @Valid @RequestBody UpdateStockRequest request) {
        return useCase.updateProductStock(franchiseId, branchId, productId, request.stock())
                .map(FranchiseWebMapper::toResponse);
    }

    @PatchMapping("/{franchiseId}/branches/{branchId}/products/{productId}/name")
    @Operation(summary = "Actualizar el nombre de un producto")
    public Mono<FranchiseResponse> renameProduct(@PathVariable String franchiseId,
                                                 @PathVariable String branchId,
                                                 @PathVariable String productId,
                                                 @Valid @RequestBody NameRequest request) {
        return useCase.renameProduct(franchiseId, branchId, productId, request.name())
                .map(FranchiseWebMapper::toResponse);
    }

    // ---------- Consultas ----------

    @GetMapping("/{franchiseId}/top-stock-products")
    @Operation(summary = "Producto con mas stock por sucursal para una franquicia",
            description = "Devuelve, por cada sucursal de la franquicia, el producto con mayor stock "
                    + "indicando a que sucursal pertenece. Las sucursales sin productos no aparecen.")
    public Flux<BranchTopProductResponse> topStockProducts(@PathVariable String franchiseId) {
        return useCase.topStockProductPerBranch(franchiseId)
                .map(FranchiseWebMapper::toResponse);
    }
}
