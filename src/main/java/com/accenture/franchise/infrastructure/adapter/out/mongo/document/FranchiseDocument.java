package com.accenture.franchise.infrastructure.adapter.out.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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

    /**
     * Version del documento para el bloqueo optimista.
     *
     * <p>Spring Data la incrementa en cada guardado y anade la version esperada
     * a la condicion del update. Si otra escritura se adelanto, la condicion no
     * encuentra el documento y el guardado falla en lugar de pisar el cambio
     * ajeno. Con la version en {@code null} el guardado se trata como insercion,
     * que es justo lo que se quiere al crear la franquicia.</p>
     */
    @Version
    private Long version;

    public FranchiseDocument() {
    }

    public FranchiseDocument(String id, String name, List<BranchDocument> branches, Long version) {
        this.id = id;
        this.name = name;
        this.branches = branches;
        this.version = version;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
