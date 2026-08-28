package com.accenture.franchise.domain.model;

import com.accenture.franchise.domain.exception.DuplicateNameException;
import com.accenture.franchise.domain.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Franquicia: un nombre y su lista de sucursales. Es la raiz del agregado.
 *
 * <p>Toda modificacion sobre sucursales o productos entra por aqui, de modo que
 * la franquicia completa se lee, se transforma y se vuelve a guardar como una
 * sola unidad. Encaja con el modelo de documento embebido de Mongo, donde la
 * franquicia es un unico documento.</p>
 *
 * @param id       identificador de la franquicia; {@code null} mientras no se persiste
 * @param name     nombre de la franquicia
 * @param branches sucursales; nunca nulo, siempre copia defensiva
 */
public record Franchise(String id, String name, List<Branch> branches) {

    public Franchise {
        name = Names.require(name, "franchise.name");
        branches = branches == null ? List.of() : List.copyOf(branches);
    }

    /**
     * Crea una franquicia nueva, sin sucursales, con identificador generado.
     *
     * <p>El id se genera en el dominio y no lo delega Mongo: asi el modelo es
     * independiente del motor de persistencia y el caso de uso puede devolver
     * el identificador sin depender del adaptador.</p>
     */
    public static Franchise create(String name) {
        return new Franchise(UUID.randomUUID().toString(), name, List.of());
    }

    public Franchise renameTo(String newName) {
        return new Franchise(id, newName, branches);
    }

    public Optional<Branch> findBranch(String branchId) {
        return branches.stream().filter(b -> b.id().equals(branchId)).findFirst();
    }

    /**
     * Agrega una sucursal.
     *
     * @throws DuplicateNameException si ya existe otra sucursal con ese nombre
     */
    public Franchise addBranch(Branch branch) {
        if (branches.stream().anyMatch(b -> b.hasName(branch.name()))) {
            throw DuplicateNameException.branch(branch.name());
        }
        List<Branch> updated = new ArrayList<>(branches);
        updated.add(branch);
        return new Franchise(id, name, updated);
    }

    /**
     * Cambia el nombre de una sucursal validando que no colisione con otra.
     */
    public Franchise renameBranch(String branchId, String newName) {
        boolean taken = branches.stream()
                .anyMatch(b -> !b.id().equals(branchId) && b.hasName(newName));
        if (taken) {
            throw DuplicateNameException.branch(newName);
        }
        return applyToBranch(branchId, branch -> branch.renameTo(newName));
    }

    public Franchise addProduct(String branchId, Product product) {
        return applyToBranch(branchId, branch -> branch.addProduct(product));
    }

    public Franchise removeProduct(String branchId, String productId) {
        return applyToBranch(branchId, branch -> branch.removeProduct(productId));
    }

    public Franchise updateProductStock(String branchId, String productId, int newStock) {
        return applyToBranch(branchId, branch -> branch.updateProductStock(productId, newStock));
    }

    public Franchise renameProduct(String branchId, String productId, String newName) {
        return applyToBranch(branchId, branch -> branch.renameProduct(productId, newName));
    }

    /**
     * Producto con mayor stock de cada sucursal, indicando a que sucursal pertenece.
     *
     * <p>Las sucursales sin productos no aparecen en el resultado.</p>
     */
    public List<BranchTopProduct> topStockProductPerBranch() {
        return branches.stream()
                .flatMap(branch -> branch.topStockProduct()
                        .map(product -> new BranchTopProduct(branch.id(), branch.name(), product))
                        .stream())
                .toList();
    }

    /** Aplica una operacion sobre una sucursal y devuelve la franquicia resultante. */
    private Franchise applyToBranch(String branchId, UnaryOperator<Branch> operation) {
        Branch current = findBranch(branchId).orElseThrow(() -> NotFoundException.branch(branchId));
        Branch modified = operation.apply(current);
        List<Branch> updated = branches.stream()
                .map(b -> b.id().equals(branchId) ? modified : b)
                .toList();
        return new Franchise(id, name, updated);
    }
}
