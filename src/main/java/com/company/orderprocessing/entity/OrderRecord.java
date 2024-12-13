package com.company.orderprocessing.entity;

public record OrderRecord(
        String customer,
        String address,
        ItemRecord item,
        Integer quantity
) {
}
