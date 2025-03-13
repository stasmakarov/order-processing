package com.company.orderprocessing.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum ItemOperation implements EnumClass<Integer> {

    RESERVATION(10),
    CANCEL_RESERVATION(20),
    DELIVERY(30),
    PRODUCTION(40);

    private final Integer id;

    ItemOperation(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static ItemOperation fromId(Integer id) {
        for (ItemOperation at : ItemOperation.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}