package com.company.orderprocessing.entity;

public record OrderRecord(
        String customer,
        String address,
        String item,
        Integer quantity
) {
}
