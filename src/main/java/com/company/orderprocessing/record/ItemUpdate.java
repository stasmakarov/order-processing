package com.company.orderprocessing.record;

import java.util.Objects;

public class ItemUpdate {
    private String id;
    private String name;
    private int quantity;
    private int operation;

    public ItemUpdate() {}

    public ItemUpdate(
            String id,
            String name,
            int quantity,
            int operation) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.operation = operation;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getOperation() {
        return operation;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }
}
