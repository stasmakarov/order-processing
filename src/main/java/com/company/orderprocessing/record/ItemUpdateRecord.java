package com.company.orderprocessing.record;

public record ItemUpdateRecord(
        String id,
        String name,
        int quantity,
        int operation) {}
