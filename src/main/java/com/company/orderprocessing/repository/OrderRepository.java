package com.company.orderprocessing.repository;

import com.company.orderprocessing.entity.Order;
import io.jmix.core.repository.JmixDataRepository;

public interface OrderRepository extends JmixDataRepository<Order, Integer> {
    @Override
    void deleteById(Integer integer);
}