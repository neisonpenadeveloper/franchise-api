package com.accenture.franchise.infrastructure.adapter.out.mongo.document;

import java.util.List;

/** Sucursal embebida dentro del documento de franquicia. */
public class BranchDocument {

    private String id;
    private String name;
    private List<ProductDocument> products;

    public BranchDocument() {
    }

    public BranchDocument(String id, String name, List<ProductDocument> products) {
        this.id = id;
        this.name = name;
        this.products = products;
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

    public List<ProductDocument> getProducts() {
        return products;
    }

    public void setProducts(List<ProductDocument> products) {
        this.products = products;
    }
}
