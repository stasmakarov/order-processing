package com.company.orderprocessing.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum OrderStatus implements EnumClass<Integer> {

    NEW(10),
    VERIFIED(20),
    READY(30),
    IN_DELIVERY(40),
    COMPLETED(50),
    CANCELLED(60);

    private final Integer id;

    OrderStatus(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static OrderStatus fromId(Integer id) {
        for (OrderStatus at : OrderStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}