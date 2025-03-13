package com.company.orderprocessing.record;

public record OrderRecord(
        String customer,
        String address,
        String item,
        Integer quantity
) {
}
