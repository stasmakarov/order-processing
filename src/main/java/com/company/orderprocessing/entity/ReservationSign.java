package com.company.orderprocessing.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum ReservationSign implements EnumClass<Integer> {

    PLUS(10),
    MINUS(20);

    private final Integer id;

    ReservationSign(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static ReservationSign fromId(Integer id) {
        for (ReservationSign at : ReservationSign.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}