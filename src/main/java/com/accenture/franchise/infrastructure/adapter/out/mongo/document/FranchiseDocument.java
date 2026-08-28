package com.accenture.franchise.infrastructure.adapter.out.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Documento de Mongo de la franquicia.
 *
 * <p>Sucursales y productos van embebidos, no en colecciones aparte: siempre se
 * leen y se escriben junto con la franquicia (es la raiz del agregado), asi que
 * embeberlos evita joins y hace que cada operacion sea una sola escritura
 * atomica sobre un documento.</p>
 *
 * <p>Vive en infraestructura y es distinto del modelo de dominio a proposito:
 * las anotaciones de Spring Data no deben filtrarse hacia adentro.</p>
 */
@Document(collection = "franchises")
public class FranchiseDocument {

    @Id
    private String id;

    /** Indice unico: no se admiten dos franquicias con el mismo nombre. */
    @Indexed(unique = true)
    private String name;

    private List<BranchDocument> branches;

    public FranchiseDocument() {
    }

    public FranchiseDocument(String id, String name, List<BranchDocument> branches) {
        this.id = id;
        this.name = name;
        this.branches = branches;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BranchDocument> getBranches() {
        return branches;
    }

    public void setBranches(List<BranchDocument> branches) {
        this.branches = branches;
    }
}
