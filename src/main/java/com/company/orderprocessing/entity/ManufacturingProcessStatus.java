package com.company.orderprocessing.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum ManufacturingProcessStatus implements EnumClass<Integer> {

    NOT_STARTED(10),
    PRODUCTION(20),
    SUSPENDED(30);

    private final Integer id;

    ManufacturingProcessStatus(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static ManufacturingProcessStatus fromId(Integer id) {
        for (ManufacturingProcessStatus at : ManufacturingProcessStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}